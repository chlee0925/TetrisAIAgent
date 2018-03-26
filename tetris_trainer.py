import random
import sys
import subprocess
from deap import base
from deap import creator
from deap import tools


# http://deap.readthedocs.io/en/master/api/creator.html
# Creator is a meta-factory that creates classes
creator.create("FitnessMax", base.Fitness, weights=(1.0,))
creator.create("Individual", list, fitness=creator.FitnessMax)

# Constants
NUMBER_OF_WEIGHTS = 22
MUTATION_GENE_RATE = 0.05
MUTATION_GENE_INDIVIDUAL_RATE = 0.2
CROSSOVER_RATE = 0.5
GENERATION_COUNT = 100
POPULATION_SIZE = 100
class GeneticAlgorithmRunner:
    def __init__(self):
        self._init_toolbox()

    # Change the genetic algorithm here for optimisation    
    def _init_toolbox(self):
        self._toolbox = base.Toolbox()
        # define 'attr_bool' to be an attribute ('gene') which corresponds to integers sampled uniformly from the range [0,1]
        self._toolbox.register("attr_bool", random.uniform, 0, 1)
        self._toolbox.register("individual", tools.initRepeat, creator.Individual, self._toolbox.attr_bool, NUMBER_OF_WEIGHTS)
        # define the population to be a list of individuals
        self._toolbox.register("population", tools.initRepeat, list, self._toolbox.individual)
        # register the goal / fitness function
        self._toolbox.register("evaluate", self.fitness_function)
        # register the crossover operator
        self._toolbox.register("mate", tools.cxTwoPoint)
        # register a mutation operator with a probability to flip each attribute/gene with probability
        self._toolbox.register("mutate", tools.mutFlipBit, indpb=MUTATION_GENE_RATE)
        self._toolbox.register("select", tools.selTournament, tournsize=3)

    def init_population(self):
        return self._toolbox.population(n=POPULATION_SIZE)
    
    def evaluate_population(self, pop):
        fitnesses = list(map(self._toolbox.evaluate, pop))
        for ind, fit in zip(pop, fitnesses):
            ind.fitness.values = fit
        return pop

    def run(self, pop):
        for i in range(GENERATION_COUNT):
            print("-- Generation "+ str(i+1) + "--")
            # Select the next generation individuals
            offspring = self._toolbox.select(pop, len(pop))
            offspring = list(map(self._toolbox.clone, offspring))
            for child1, child2 in zip(offspring[::2], offspring[1::2]):
                # cross two individuals with probability CXPB
                if random.random() < CROSSOVER_RATE:
                    self._toolbox.mate(child1, child2)

                    # fitness values of the children
                    # must be recalculated later
                    del child1.fitness.values
                    del child2.fitness.values
            for mutant in offspring:
                # mutate an individual with probability MUTPB
                if random.random() < MUTATION_GENE_INDIVIDUAL_RATE:
                    self._toolbox.mutate(mutant)
                    del mutant.fitness.values
            # Evaluate the individuals with an invalid fitness
            invalid_ind = [ind for ind in offspring if not ind.fitness.valid]
            fitnesses = map(self._toolbox.evaluate, invalid_ind)
            for ind, fit in zip(invalid_ind, fitnesses):
                ind.fitness.values = fit
            print("  Evaluated %i individuals" % len(invalid_ind))
        
            # The population is entirely replaced by the offspring
            pop[:] = offspring
        
            # Gather all the fitnesses in one list and print the stats
            fits = [ind.fitness.values[0] for ind in pop]
        
            length = len(pop)
            mean = sum(fits) / length
            sum2 = sum(x*x for x in fits)
            std = abs(sum2 / length - mean**2)**0.5
        
            print("  Min %s" % min(fits))
            print("  Max %s" % max(fits))
            print("  Avg %s" % mean)
            print("  Std %s" % std)
            best_ind = tools.selBest(pop, 1)[0]
            print("  Best individual is %s" %(best_ind))
    
    def fitness_function(self, individual):
        result = []
        for _ in range(3):
            result.append(int(subprocess.check_output(['java', '-classpath', "out/", "NoVisualPlayerSkeleton"] + [str(x) for x in individual]).strip()))
        return (reduce(lambda x, y: x + y, result) / len(result)),

def set_random_seed():
    random.seed(64)

def main():
    set_random_seed()
    genetic_algo = GeneticAlgorithmRunner()
    pop = genetic_algo.init_population()
    
    print("Start of evolution")
    
    # Evaluate the entire population
    pop = genetic_algo.evaluate_population(pop)

    genetic_algo.run(pop)

if __name__ == "__main__":
    try:
        main()
    except:
        print("Error found", sys.exc_info()[0])
        sys.exit()