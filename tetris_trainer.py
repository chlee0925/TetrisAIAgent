import random
import sys
import subprocess
import math
from threading import Thread
from pickle import load
from pickle import dump

class Individual(list):
    def __init__(self, fitness=-1, min=-1, max=-1, std_dev=-1):
        self.fitness = fitness
        self.min = min
        self.max = max
        self.std_dev = std_dev

# Constants
MULTI_THREADING = True
NUMBER_OF_WEIGHTS = 12
MUTATION_GENE_INDIVIDUAL_RATE = 0.2
CROSSOVER_RATE = 0.5
GENERATION_COUNT = 100
POPULATION_SIZE = 1000
# We will invoke the java player 5 times, in which each java player will run 10 games
# We will have statistics of 50 games in the end
PLAYER_INVOKE_AMOUNT = 5
LAST_GENERATION_FILE_NAME = "last_gen.pickle"
NUMBER_OF_EVALUATING_POP_PER_BATCH = 10
GENERATION_DIR = "generations/"

# If the individual is able to clear at least TURN * 0.4 - NEGLECTABLE_ERROR, then this individual can be
# categorised as well performing individual
TURN = 1000
NEGLECTABLE_ERROR = 15

class GeneticAlgorithmRunner:
    def __init__(self, timestamp):
        self._timestamp = timestamp

    def init_population(self):
        pop = []
        for _ in range(POPULATION_SIZE):
            random_weights = []
            for _ in range(NUMBER_OF_WEIGHTS):
                random_weights.append(random.random())
            ind = Individual()
            ind.extend(random_weights)
            pop.append(ind)
        return pop
    
    def weighted_average_crossover(self, ind1, ind2):
        total_ratio = ind1.fitness + ind2.fitness
        ind1_ratio = float(ind1.fitness) / total_ratio
        ind2_ratio = float(ind2.fitness) / total_ratio
        new_weight = []
        for ind1_weight, ind2_weight in zip(ind1, ind2):
            new_weight.append(ind1_ratio * ind1_weight + ind2_ratio * ind2_weight)
        new_ind = Individual()
        new_ind.extend(new_weight)
        return new_ind

    def selection(self, pop, tournsize=10):
        top_size = int(POPULATION_SIZE * 0.7)
        pop = sorted(pop, key=lambda x: (x.fitness, -x.std_dev), reverse=True)
        new_size = POPULATION_SIZE - top_size
        del pop[-new_size:]
        offspring_pop = []
        for i in xrange(new_size):
            aspirants = random.sample(pop, tournsize)
            top_k = sorted(aspirants, key=lambda x: (x.fitness, -x.std_dev), reverse=True)[:2]
            offspring = self.weighted_average_crossover(top_k[0], top_k[1])
            offspring_pop.append(offspring)
        return (pop, offspring_pop)

    # Given a population, calculate fitness for every single individual
    def evaluate_population(self, pop):
        fitness_stats = list(self.map_evaluate(pop))
        for ind, fit in zip(pop, fitness_stats):
            ind.fitness = fit[0]
            ind.min = fit[1]
            ind.max = fit[2]
            ind.std_dev = fit[3]
        return pop

    def map_evaluate(self, pop):
        results = [None] * len(pop)
        if MULTI_THREADING:
            total_length_of_unprocessed_pop = len(pop)
            iter_count = 0
            while total_length_of_unprocessed_pop > 0:
                threads = [None] * min(total_length_of_unprocessed_pop, NUMBER_OF_EVALUATING_POP_PER_BATCH)
                for i in range(len(threads)):
                    count = iter_count * NUMBER_OF_EVALUATING_POP_PER_BATCH + i
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
                random_index = int(len(mutant) * random.random())
                mutant[random_index] = max(0, min(mutant[random_index] + (random.random() * 2 / 5) - 0.2, 1))
        return pop

    def update_turn(self, pop):
        global TURN
        fitness_list = sorted(list(map(lambda x: x.fitness, pop)))
        if fitness_list[int(POPULATION_SIZE * 0.5)] >= (TURN * 0.4) - NEGLECTABLE_ERROR:
            print("-- More than half of the population are well performing --")
            print("-- Doubling turn limit and re-evaluating population")
            TURN *= 2
            return self.evaluate_population(pop)
        return pop

    def run(self, pop):
        for i in range(GENERATION_COUNT):
            print("-- Generation "+ str(i+1) + "--")
            print("-- Number of turns: " + str(TURN) + "--")
            pop, offspring = self.selection(pop, tournsize=10)
            offspring = self.mutate(offspring)
            offspring = self.evaluate_population(offspring)
            pop.extend(offspring)
            self.report_current_generation(pop)
            self.saves_gen_into_disk(pop, GENERATION_DIR, i)
            pop = self.update_turn(pop)
        return pop

    # Change this function to alter the generation reporting
    def report_current_generation(self, pop):
        # Gather all the fitnesses in one list and print the stats
        fits = [ind.fitness for ind in pop]
        length = len(pop)
        mean = sum(fits) / length
        # sum2 = sum(x*x for x in fits)
        # std = abs(sum2 / length - mean**2)**0.5
        reporting_pop = sorted(pop, key=lambda x: (x.fitness, -x.std_dev), reverse=True)
        print("  Population Min %s" % reporting_pop[len(reporting_pop)-1].fitness)
        best_ind = reporting_pop[0]
        print("  Max %s" % best_ind.fitness)
        print("  Population Avg %s" % mean)
        print("  Max individual Std %s" % best_ind.std_dev)
        print("  Best individual is %s" %(best_ind))

    # load the generation from disk using pickle
    def load_gen_from_disk(self, file_name):
        return load(open(file_name, "rb"))

    # Saves the generation into disk using pickle
    def saves_gen_into_disk(self, pop, dir, i):
        dump(pop, open(dir + self._timestamp + "/" + str(i+1) + ".pickle", "wb"))

    def thread_fitness_function(self, individual, results, i):
        results[i] = int(subprocess.check_output(['java', '-classpath', "out/", "NoVisualPlayerSkeleton", "1", str(TURN)] + [str(x) for x in individual]).strip())
    
    # Given an individual, calculate its fitness value
    def fitness_function(self, individual):
        result = []
        results = [None] * PLAYER_INVOKE_AMOUNT
        for i in range(PLAYER_INVOKE_AMOUNT):
            self.thread_fitness_function(individual, results, i)
        mean = reduce(lambda x, y: x + y, results) / len(results)
        sum2 = sum(x*x for x in results)
        std = abs(sum2 / len(results) - mean**2)**0.5
        return (mean,min(results), max(results), std)

def main(timestamp):
    genetic_algo = GeneticAlgorithmRunner(timestamp)
    pop = genetic_algo.init_population()
    # pop = genetic_algo.load_gen_from_disk(LAST_GENERATION_FILE_NAME)

    print("Start of evolution")
    
    # Evaluate the entire population
    pop = genetic_algo.evaluate_population(pop)
    # Run the genetic algorithm and returns the last generation
    pop = genetic_algo.run(pop)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Please input the timestamp")
        sys.exit()
    timestamp = sys.argv[1]
    try:
        main(timestamp)
    except:
        print("Error found", sys.exc_info()[0])
        sys.exit()