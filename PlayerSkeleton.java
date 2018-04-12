import java.util.*;
import java.util.stream.*;

public class PlayerSkeleton {
	public static final int COLS = 10;
	public static final int ROWS = 21;
	public static final int N_PIECES = 7;
    public double[] weightVectors = new double[]{ 
                                                // Reward
                                                20.0
        
                                                // Features
                                                , 1.0, 1.0, 1.0, 1.0, 1.0 
                                                , 1.0, 1.0, 1.0, 1.0, 1.0
                                                , 1.0, 1.0, 1.0, 1.0, 1.0
                                                , 1.0, 1.0, 1.0, 1.0, 1.0
                                                , 2.0};

	public PlayerSkeleton() {
	}

	public PlayerSkeleton(ArrayList<Double> weightOverride) {
        for(int i = 0; i < weightOverride.size(); i++) if (i < this.weightVectors.length) this.weightVectors[i]=weightOverride.get(i);
	}

	public int pickMove(State s, int[][] legalMoves) {
		return pickMoveImpl(s.getField(), legalMoves, s.getTop(), s.getpOrients(), s.getpWidth(), s.getpHeight(), s.getpBottom(), s.getpTop(), s.getNextPiece());
	}

	public static void main(String[] args) {
		State s = new State();
        new TFrame(s);

        // Override weight vectors if provided
        PlayerSkeleton p = new PlayerSkeleton(Arrays.asList(args).stream().mapToDouble(weightStr -> Double.parseDouble(weightStr)).boxed().collect(Collectors.toCollection(ArrayList::new)));

        // Print out the weight vectors used
		for(int wIndex = 0; wIndex < p.weightVectors.length; wIndex++) {
			System.out.print(p.weightVectors[wIndex]+",");
        }
        System.out.println();
        
		while (!s.hasLost()) {
			s.makeMove(p.pickMove(s, s.legalMoves()));
			s.draw();
			s.drawNext(0, 0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed " + s.getRowsCleared() + " rows.");
	}

	/**
	 * Implementation method for pickMove
	 * for every possible legal move, try making that move and run the evaluation function on that state.
	 */
	public int pickMoveImpl(int[][] field, int[][] legalMoves, int[] top, int[] pOrient, int[][] pWidth, int[][] pHeight, int[][][] pBottom, int[][][] pTop, int nextPiece) {
		int moveDecision = 0;
        double currentBest = Double.NEGATIVE_INFINITY;
        
		for (int moveIndex = 0; moveIndex < legalMoves.length; moveIndex++) {
			int[] move = legalMoves[moveIndex];
			int orient = move[0];
            int slot = move[1];
            
            // Create temporary field to simulate a move
            int[][] tempField = new int[field.length][];
            for(int i = 0; i < tempField.length; i++) {
                tempField[i] = Arrays.copyOf(field[i], field[i].length);
            }
            // Create temporary top to simulate a move
			int[] tempTop = Arrays.copyOf(top, top.length);

			///////////////
			///Make Move///
			///////////////

			// Find height where the piece is placed at
			int height = tempTop[slot] - pBottom[nextPiece][orient][0];
			// For each column beyond the first in the piece
			for (int c = 1; c < pWidth[nextPiece][orient]; c++) {
				height = Math.max(height, tempTop[slot + c] - pBottom[nextPiece][orient][c]);
			}

			// If the game is going to end, try the next move
			if (height + pHeight[nextPiece][orient] >= ROWS) {
				continue;
			}

			// for each column in the piece - fill in the appropriate blocks
			for (int i = 0; i < pWidth[nextPiece][orient]; i++) {
				//from bottom to top of brick
				for (int h = height + pBottom[nextPiece][orient][i]; h < height + pTop[nextPiece][orient][i]; h++) {
					tempField[h][i + slot] = Integer.MAX_VALUE;
				}
			}
			
			//adjust tempoaray top
			for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
				tempTop[slot+c]=height+pTop[nextPiece][orient][c];
            }

            int rowsCleared = 0;
            
            //check for full rows - starting at the top
            for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
                //check all columns in the row
                boolean full = true;
                for(int c = 0; c < COLS; c++) {
                    if(tempField[r][c] == 0) {
                        full = false;
                        break;
                    }
                }
                //if the row was full - remove it and slide above stuff down
                if(full) {
                    rowsCleared++;
                    //for each column
                    for(int c = 0; c < COLS; c++) {
                        //slide down all bricks
                        for(int i = r; i < tempTop[c]; i++) {
                            tempField[i][c] = tempField[i+1][c];
                        }
                        //lower the top
                        tempTop[c]--;
                        while(tempTop[c]>=1 && tempField[tempTop[c]-1][c]==0) tempTop[c]--;
                    }
                }
            }

			/////////////////////////////////
			///Run the evaluation function///
			/////////////////////////////////

			// Calculate the evaluation value
			double evaluationValue = evaluationFunction(tempField, tempTop, rowsCleared);
			if (evaluationValue > currentBest) {
				currentBest = evaluationValue;
				moveDecision = moveIndex;
			}
		}

		return moveDecision;
	}

	public double evaluationFunction(int[][] field, int[] top, int rowsCleared) {
		final int featureColumnHeightIndex = 1;
		final int featureAbsoluteAdjColumnHeightDiffIndex = 11;

		return

			// INDEX 0 - REWARD
			(weightVectors[0]) * rowsCleared

			// FEATURE 1~10 - COLUMN HEIGHT
			- (IntStream.rangeClosed(0,9).mapToDouble(col -> (weightVectors[featureColumnHeightIndex+col]) * featureColumnHeight(top, col)).sum())

			// FEATURE 11~19 - ABSOLUTE HEIGHT DIFF
			- (IntStream.rangeClosed(0,8)
				.mapToDouble(col -> (weightVectors[featureAbsoluteAdjColumnHeightDiffIndex+col]) * featureAbsoluteAdjColumnHeightDiff(top, col)).sum())

			// FEATURE 20 - MAX HEIGHT
			- (weightVectors[20]) * featureMaxColumnHeight(top)

			// FEATURE 21 - NUM OF HOLES
			- (weightVectors[21]) * featureNumOfHoles(field, top);
	}

	///////////////////////////////////////
	///////////		FEATURES	///////////
	///////////////////////////////////////

	/**
	 * FEATURE 1~10 - Column Height
	 */
	public int featureColumnHeight(int[] top, int col) {
		return top[col];
	}

	/**
	 * FEATURE 11~19 - Absolute height difference between (col) and (col+1) columns.
	 */
	public int featureAbsoluteAdjColumnHeightDiff(int[] top, int col) {
		return Math.abs(top[col] - top[col+1]);
	}

	/**
	 * FEATURE 20 - Maximum height across all columns
	 */
	public int featureMaxColumnHeight(int[] top) {
		int maxHeight = 0;
		for (int height : top) {
			if (maxHeight < height) {
				maxHeight = height;
			}
		}
		return maxHeight;
	}

	/**
	 * FEATURE 21 - the number of holes in the wall, that is, the number of empty positions of
	 * the wall that have at least one full position above them.
	 */
	public int featureNumOfHoles(int[][] field, int[] top) {
		int holes = 0;
		for (int i=0; i<top.length; i++) {
			for (int j=0; j<top[i]; j++) {
				if (field[j][i] == 0) {
					holes++;
				}
			}
		}
		return holes;
	}

	/**
	 * FEATURE 5 - Depth of Wells (number of blocks in wells)
	 */
	public int featureDepthOfWells(int[] top) {
		int wells = 0;
		int depth = 0;
		for (int i=0; i<top.length; i++) {
			if (i==0) {
				depth = top[i+1] - top[i];
			} else if (i == top.length-1) {
				depth = top[i-1] - top[i];
			} else {
				depth = Math.min(top[i+1], top[i-1]) - top[i];
			}
			if(depth > 0){
				wells += depth;
			}
		}
		return wells;
	}

	//////////////////////////////////
	///////////  REWARD  /////////////
	//////////////////////////////////

	public int getMinColHeight(int[] top) {
		int minHeight = Integer.MAX_VALUE;
		for (int height : top) {
			if (minHeight > height) {
				minHeight = height;
			}
		}
		return minHeight;
	}

}
