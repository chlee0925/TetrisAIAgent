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
									, -1.0};

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
				Thread.sleep(500);
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
					field[h][i + slot] = Integer.MAX_VALUE; // TODO Change the magic number
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
		int weightIndex = 0;
		return (weightVectors[weightIndex++]) * rewardRowsToBeCleared(field)
				+ (weightVectors[weightIndex++]) * (cols.stream().mapToInt(col -> featureColumnHeight(field, col)).sum())
				+ (weightVectors[weightIndex++]) * (cols.stream().filter(col -> { return col <= 8; }).mapToInt(col -> featureAbsoluteAdjColumnHeightDiff(field, col)).sum())
				+ (weightVectors[weightIndex++]) * featureMaxColumnHeight(field) + (-1) * featureNumOfHoles(field);
	}

	///////////////////////////////////////
	///////////		FEATURES	///////////
	///////////////////////////////////////

	/**
	 * FEATURE 1~10 - Column Height
	 */
	public int featureColumnHeight(int[][] field, int col) {
		return 0; // TODO Implement
	}

	/**
	 * FEATURE 11~19 - Absolute height difference between (col) and (col+1) columns.
	 */
	public int featureAbsoluteAdjColumnHeightDiff(int[][] field, int col) {
		return 0; // TODO Implement
	}

	/**
	 * FEATURE 20 - Maximum height across all columns
	 */
	public int featureMaxColumnHeight(int[][] field) {
		return 0; // TODO Implement
	}

	/**
	 * FEATURE 21 - the number of holes in the wall, that is, the number of empty positions of 
	 * the wall that have at least one full position above them.
	 */
	public int featureNumOfHoles(int[][] field) {
		return 0; // TODO Implement
	}

	/////////////////////////////////////
	///////////		REWARD	/////////////
	/////////////////////////////////////

	public int rewardRowsToBeCleared(int[][] field) {
		return 0; // TODO Implement
	}

}
