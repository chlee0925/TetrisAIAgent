import random
import sys
import subprocess
import numpy as np
from threading import Thread
from pickle import load
from pickle import dump
from deap import base
from deap import creator
from deap import tools

# http://deap.readthedocs.io/en/master/api/creator.html
# Creator is a meta-factory that creates classes
creator.create("FitnessMax", base.Fitness, weights=(1.0,))
creator.create("Individual", list, fitness=creator.FitnessMax, min=0, max=0, std_dev=0)

# Constants
MULTI_THREADING = True
NUMBER_OF_WEIGHTS = 5
MUTATION_GENE_RATE = 0.1
MUTATION_GENE_INDIVIDUAL_RATE = 0.2
CROSSOVER_RATE = 0.5
GENERATION_COUNT = 100
POPULATION_SIZE = 50
FITNESS_FUNCTION_AVERAGE_COUNT = 3
LAST_GENERATION_FILE_NAME = "last_gen.pickle"
NUMBER_OF_EVALUATING_POP_PER_BATCH = 10

class GeneticAlgorithmRunner:
    def __init__(self):
        self._init_toolbox()

    # Change the genetic algorithm here for optimisation    
    def _init_toolbox(self):
        self._toolbox = base.Toolbox()
        # define 'attr_bool' to be an attribute ('gene') which corresponds to integers sampled uniformly from the range [0,1]
        self._toolbox.register("attr_float", random.random)
        self._toolbox.register("individual", tools.initRepeat, creator.Individual, self._toolbox.attr_float, NUMBER_OF_WEIGHTS)
        # define the population to be a list of individuals
        self._toolbox.register("population", tools.initRepeat, list, self._toolbox.individual)
        # register the goal / fitness function
        self._toolbox.register("evaluate", self.fitness_function)
        # register the crossover operator
        self._toolbox.register("mate", tools.cxOnePoint)
        # register a mutation operator with a probability to flip each attribute/gene with probability
        self._toolbox.register("mutate", tools.mutUniformInt, indpb=MUTATION_GENE_RATE, low=0, up=1)
        self._toolbox.register("select", tools.selTournament, tournsize=10)

    def init_population(self):
        return self._toolbox.population(n=POPULATION_SIZE)
    
    # One of possible select functions
    def select(self, pop):
        top_k = tools.selBest(pop, int(POPULATION_SIZE * 0.1))
        results = list(map(self._toolbox.clone, top_k))
        while len(results) < len(pop):
            results.append(self._toolbox.clone(random.choice(top_k)))
        random.shuffle(results)
        return results
    
    def weighted_average_crossover(self, ind1, ind2):
        total_ratio = ind1.fitness.values[0] + ind2.fitness.values[0]
        ind1_ratio = ind1.fitness.values[0] / total_ratio
        ind2_ratio = ind2.fitness.values[0] / total_ratio
        new_weight = ind1_ratio * np.array(ind1) + ind2_ratio * np.array(ind2)
        return creator.Individual(new_weight)

    def selectTop(self, pop, tournsize=10):
        top_size = int(POPULATION_SIZE * 0.7)
        top_pop = tools.selBest(pop, top_size)
        new_size = POPULATION_SIZE - len(top_pop)
        for i in xrange(new_size):
            aspirants = tools.selRandom(pop, tournsize)
            top_k = tools.selBest(aspirants, 2)
            offspring = self.weighted_average_crossover(top_k[0], top_k[1])
            top_pop.append(offspring)
        return top_pop

    # Given a population, calculate fitness for every single individual
    def evaluate_population(self, pop):
        fitness_stats = list(self.map_evaluate(pop))
        for ind, fit in zip(pop, fitness_stats):
            ind.fitness.values = fit[0]
            ind.min = fit[1]
            ind.max = fit[2]
            ind.std_dev = fit[3]
        return pop

    def map_evaluate(self, pop):
        results = [None] * len(pop)
        if MULTI_THREADING:
            total_length_of_unprocessed_pop = len(pop)
            iter_count = 0
            while total_length_of_unprocessed_pop != 0:
                threads = [None] * min(total_length_of_unprocessed_pop, NUMBER_OF_EVALUATING_POP_PER_BATCH)
                for i in range(len(threads)):
                    count = iter_count * NUMBER_OF_EVALUATING_POP_PER_BATCH + i
                    print(count)
                    threads[i] = Thread(target=self.map_fitness_function, args=(pop[count], results, count))
                    threads[i].start()
                for i in range(len(threads)):
                    threads[i].join()
                iter_count += 1
                total_length_of_unprocessed_pop -= NUMBER_OF_EVALUATING_POP_PER_BATCH
        else:
            for i in range(len(pop)):
                self.map_fitness_function(pop[i], results, i)
        return results

    def map_fitness_function(self, individual, results, i):
        results[i] = self.fitness_function(individual)

    def mutate(self, pop):
        for mutant in pop:
            if random.random() < MUTATION_GENE_INDIVIDUAL_RATE:
                self._toolbox.mutate(mutant)
                del mutant.fitness.values
        return pop

    def crossover(self, pop):
        for child1, child2 in zip(pop[::2], pop[1::2]):
            # cross two individuals with probability CXPB
            if random.random() < CROSSOVER_RATE:
                self._toolbox.mate(child1, child2)
                # fitness values of the children
                # must be recalculated later
                del child1.fitness.values
                del child2.fitness.values
        return pop

    def run(self, pop):
        for i in range(GENERATION_COUNT):
            print("-- Generation "+ str(i+1) + "--")
            # offspring = self._toolbox.select(pop, len(pop))
            offspring = self.selectTop(pop, tournsize=10)
            offspring = list(map(self._toolbox.clone, offspring))
            # offspring = self.crossover(offspring)
            offspring = self.mutate(offspring)
            offspring = self.evaluate_population(offspring)
            pop[:] = offspring
            self.report_current_generation(pop)
        return pop

    # Change this function to alter the generation reporting
    def report_current_generation(self, pop):
        # Gather all the fitnesses in one list and print the stats
        fits = [ind.fitness.values[0] for ind in pop]
        length = len(pop)
        mean = sum(fits) / length
        # sum2 = sum(x*x for x in fits)
        # std = abs(sum2 / length - mean**2)**0.5
        
        print("  Population Min %s" % tools.selWorst(pop, 1)[0].fitness.values[0])
        best_ind = tools.selBest(pop, 1)[0]
        print("  Max %s" % best_ind.fitness.values)
        print("  Population Avg %s" % mean)
        print("  Max individual Std %s" % best_ind.std_dev)
        print("  Best individual is %s" %(best_ind))

    # load the generation from disk using pickle
    def load_gen_from_disk(self, file_name):
        return load(open(file_name, "rb"))

    # Saves the generation into disk using pickle
    def saves_gen_into_disk(self, pop, file_name):
        dump(pop, open(file_name, "wb"))

    def thread_fitness_function(self, individual, results, i):
        results[i] = int(subprocess.check_output(['java', '-classpath', "out/", "NoVisualPlayerSkeleton"] + [str(x) for x in individual]).strip())
    
    # Given an individual, calculate its fitness value
    def fitness_function(self, individual):
        result = []
        results = [None] * FITNESS_FUNCTION_AVERAGE_COUNT
        # if MULTI_THREADING:
        #     threads = [None] * FITNESS_FUNCTION_AVERAGE_COUNT
        #     for i in range(len(threads)):
        #         threads[i] = Thread(target=self.thread_fitness_function, args=(individual, results, i))
        #         threads[i].start()

        #     for i in range(len(threads)):
        #         threads[i].join()
        # else:
        #     for i in range(FITNESS_FUNCTION_AVERAGE_COUNT):
        #         self.thread_fitness_function(individual, results, i)
        ## TODO: Implement batch processing on the java side
        for i in range(FITNESS_FUNCTION_AVERAGE_COUNT):
            self.thread_fitness_function(individual, results, i)
        mean = reduce(lambda x, y: x + y, results) / len(results)
        sum2 = sum(x*x for x in results)
        std = abs(sum2 / len(results) - mean**2)**0.5
        return ((mean,),min(results), max(results), std)

def set_random_seed():
    random.seed(64)

def main():
    set_random_seed()
    genetic_algo = GeneticAlgorithmRunner()
    pop = genetic_algo.init_population()
    # pop = genetic_algo.load_gen_from_disk(LAST_GENERATION_FILE_NAME)

    print("Start of evolution")
    
    # Evaluate the entire population
    pop = genetic_algo.evaluate_population(pop)

    # Run the genetic algorithm and returns the last generation
    pop = genetic_algo.run(pop)
    genetic_algo.saves_gen_into_disk(pop, LAST_GENERATION_FILE_NAME)

if __name__ == "__main__":
    try:
        main()
    except:
        print("Error found", sys.exc_info()[0])
        sys.exit()