import sys
import subprocess
import os.path

ROUNDS_OF_TRAINING = 3
ROUNDS_FOR_AVERAGE = 10
JAVA_OUTPUT_DIR = "out/"
PLAYER_SKELETON_CLASS = "NoVisualPlayerSkeleton"
class TetrisTrainer:

    def __init__(self, number_of_rounds, average_rounds):
        self._number_of_rounds = number_of_rounds
        self._average_rounds = average_rounds
    
    def train(self):
        if os.path.isfile(JAVA_OUTPUT_DIR + PLAYER_SKELETON_CLASS):
            print("Please compile the java before training")
            sys.exit()
        for i in range(self._number_of_rounds):
            # TODO: Generate weights / Mutation
            current_round_raw_data = []
            for _ in range(self._average_rounds):
                current_round_raw_data.append(int(subprocess.check_output(['java', '-classpath', JAVA_OUTPUT_DIR, PLAYER_SKELETON_CLASS]).strip()))
            print("Round " + str(i + 1) + " average rows cleared: "+ str(reduce(lambda x, y: x + y, current_round_raw_data) / len(current_round_raw_data)))

if __name__ == "__main__":
    tt = TetrisTrainer(ROUNDS_OF_TRAINING, ROUNDS_FOR_AVERAGE)
    tt.train()
