import java.util.*;

public class PlayerSkeleton {

	List<Integer> cols = Arrays.asList(new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
	public static final int COLS = 10;
	public static final int ROWS = 21;
	public static final int N_PIECES = 7;

	public double[] weightVectors = {20.0 // Reward
									, -1.0, -1.0, -1.0, -1.0, -1.0 // Features
									, -1.0, -1.0, -1.0, -1.0, -1.0
									, -1.0, -1.0, -1.0, -1.0, -1.0
									, -1.0, -1.0, -1.0, -1.0, -1.0
									, -2.0
									, -1.0, -1.0, -1.0 // Additional Features
									};

	public int pickMove(State s, int[][] legalMoves) {
		return pickMoveImpl(s.getField(), legalMoves, s.getTop(), s.getpOrients(), s.getpWidth(), s.getpHeight(), s.getpBottom(), s.getpTop(), s.getNextPiece());
	}

	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
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
		final int featureColumnHeightIndex = 1;
		final int featureAbsoluteAdjColumnHeightDiffIndex = 11;

		return

			// INDEX 0 - REWARD
			(weightVectors[0]) * rewardRowsToBeCleared(field, top)

			// FEATURE 1~10 - COLUMN HEIGHT
			+ (cols.stream().mapToDouble(col -> (weightVectors[featureColumnHeightIndex+col]) * featureColumnHeight(top, col)).sum())

			// FEATURE 11~19 - ABSOLUTE HEIGHT DIFF
			+ (cols.stream().filter(col -> { return col <= 8; })
				.mapToDouble(col -> (weightVectors[featureAbsoluteAdjColumnHeightDiffIndex+col]) * featureAbsoluteAdjColumnHeightDiff(top, col)).sum())

			// FEATURE 20 - MAX HEIGHT
			+ (weightVectors[20]) * featureMaxColumnHeight(top)

			// FEATURE 21 - NUM OF HOLES
			+ (weightVectors[21]) * featureNumOfHoles(field, top);

			// ADDITIONAL FEATURES
		//	+ (weightVectors[22]) * featureHeightWeightedCells(field, top);
		//	+ (weightVectors[23]) * featureDepthOfWells(field, top);
		//	+ (weightVectors[24]) * featureNumOfFullCells(field, top);
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
				if (field[j][i] == 0 && field[j+1][i] != 0) {
					holes++;
				}
			}
		}
		return holes;
	}

	/**
	 * ADDITIONAL FEATURE - Height weighted cells: Full cells weighted by their height
	 */
	 public int featureHeightWeightedCells(int[][] field, int[] top) {
		 int sum = 0;
		 for (int i=0; i<top.length; i++) {
 			for (int j=0; j<top[i]; j++) {
				if (field[j][i] != 0) sum += j + 1; // weight of row 0 = 1
 			}
 		}
		return sum;
	 }

	 /**
 	 * ADDITIONAL FEATURE - Wells: Sum of the depth of the wells
 	 */
	 public int featureDepthOfWells(int[][] field, int[] top) {
		 // need a clearer definition of wells
		 return 0;
	 }

	 /**
 	 * ADDITIONAL FEATURE - Full cells: Number of occupied cells on the board
 	 */
	 public int featureNumOfFullCells(int[][] field, int[] top) {
		 int fullCells = 0;
		 for (int i=0; i<top.length; i++) {
			 for (int j=0; j<top[i]; j++) {
				 if (field[j][i] != 0) {
					 fullCells++;
				 }
			 }
 		}
 		return fullCells;
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
