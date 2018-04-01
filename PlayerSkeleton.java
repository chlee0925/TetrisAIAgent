import java.util.*;
import java.util.stream.*;

public class PlayerSkeleton {
	public static final int COLS = 10;
	public static final int ROWS = 21;
	public static final int N_PIECES = 7;
	public double[] weightVectors = new double[]{ 
												// Reward
												0.760666 // Complete lines

												// Features
												, 0.510066 // Aggregate Height
												, 0.184483 // Bumpiness
												, 0.100000 // Maximum height
												, 0.356630 // Num holes
												// , 1.0   // Additional Features
												};

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
		boolean training = true;
		while (!s.hasLost()) {
			if(training && s.getTurnNumber() > 500) {
				break;
			}
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

			///////////////
			///Make Move///
			///////////////

			// Find height where the piece is placed at
			int height = top[slot] - pBottom[nextPiece][orient][0];
			//for each column beyond the first in the piece
			for (int c = 1; c < pWidth[nextPiece][orient]; c++) {
				height = Math.max(height, top[slot + c] - pBottom[nextPiece][orient][c]);
			}

			// If the game is going to end, try the next move
			if (height + pHeight[nextPiece][orient] >= ROWS) {
				continue;
			}

			// for each column in the piece - fill in the appropriate blocks
			for (int i = 0; i < pWidth[nextPiece][orient]; i++) {
				//from bottom to top of brick
				for (int h = height + pBottom[nextPiece][orient][i]; h < height + pTop[nextPiece][orient][i]; h++) {
					field[h][i + slot] = Integer.MAX_VALUE;
				}
			}
			// Create temporary top
			int[] tempTop = Arrays.copyOf(top, top.length);
			//adjust tempoaray top
			for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
				tempTop[slot+c]=height+pTop[nextPiece][orient][c];
			}

			/////////////////////////////////
			///Run the evaluation function///
			/////////////////////////////////

			// Calculate the evaluation value
			double evaluationValue = evaluationFunction(field, tempTop);
			if (evaluationValue > currentBest) {
				currentBest = evaluationValue;
				moveDecision = moveIndex;
			}

			////////////////////////////////
			///Restore the original state///
			////////////////////////////////

			// Restore the original field
			for (int i = 0; i < pWidth[nextPiece][orient]; i++) {
				//from bottom to top of brick
				for (int h = height + pBottom[nextPiece][orient][i]; h < height + pTop[nextPiece][orient][i]; h++) {
					field[h][i + slot] = 0; // Undo
				}
			}
		}

		return moveDecision;
	}

	public double evaluationFunction(int[][] field, int[] top) {

		return
			// FEATURE 0 - ROWS CLEARED
			(weightVectors[0]) * rewardRowsToBeCleared(field, top)
			// FEATURE 1 - COLUMN HEIGHT
			- (weightVectors[1]) * featureAggregateHeight(top)
			// FEATURE 2 - BUMPINESS
			- (weightVectors[2]) * featureBumpiness(top)
			// FEATURE 3 - MAX HEIGHT
			- (weightVectors[3]) * featureMaxColumnHeight(top)
			// FEATURE 4 - NUM OF HOLES
			- (weightVectors[4]) * featureNumOfHoles(field, top);

			// ADDITIONAL FEATURES
		//	- (weightVectors[5]) * featureHeightWeightedCells(field, top);
	}

	///////////////////////////////////////
	///////////		FEATURES	///////////
	///////////////////////////////////////

	/**
	 * helper FEATURE 1 - column height
	 */
	public int featureColumnHeight(int[] top, int col) {
		return top[col];
	}

	/**
	 * FEATURE 1 - Aggregate Height
	 */
	public int featureAggregateHeight(int[] top) {
		int aggregateHeight = 0;
		for (int i=0; i<top.length; i++) {
			aggregateHeight += featureColumnHeight(top, i);
		}
		return aggregateHeight;
	}

	/**
	 * helper FEATURE 2 - Absolute height difference between (col) and (col+1) columns.
	 */
	public int featureAbsoluteAdjColumnHeightDiff(int[] top, int col) {
		return Math.abs(top[col] - top[col+1]);
	}

	/**
	 * FEATURE 2 - Bumpiness
	 */
	public int featureBumpiness(int[] top) {
		int bumpiness = 0;
		for (int i=0; i<top.length-1; i++) {
			bumpiness += featureAbsoluteAdjColumnHeightDiff(top, i);
		}
		return bumpiness;
	}

	/**
	 * FEATURE 3 - Maximum height across all columns
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
	 * FEATURE 4 - the number of holes in the wall, that is, the number of empty positions of
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

	public int rewardRowsToBeCleared(int[][] field, int[] top) {
		int rowsCleared = 0;
		for (int i=0; i<getMinColHeight(top); i++) {
			boolean isFullRow = true;
			for (int j=0; j<field[i].length; j++) {
				if (field[i][j] == 0) {
					isFullRow = false;
					break;
				}
			}
			if (isFullRow) {
				rowsCleared++;
			}
		}
		return rowsCleared;
	}

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
