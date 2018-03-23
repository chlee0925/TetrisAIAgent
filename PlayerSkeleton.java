import java.util.*;


public class PlayerSkeleton {

	List<Integer> cols = Arrays.asList(new Integer[]{0,1,2,3,4,5,6,7,8,9});

	public int pickMove(State s, int[][] legalMoves) {
		return pickMoveImpl(s.getField(), legalMoves);
	}
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
	// Implementation method for pickMove
	public int pickMoveImpl(int[][] field, int[][] legalMoves) {
		// TODO for every possible legal move, try making that move and run the evaluation function on that state.
		// TODO Ensure that field and levalMoves are NOT modified at the end.
		return 0;
	}

	public int evaluationFunction(int[][] field) {
		return
			(20) * rewardRowsToBeCleared(field)
			+ (-1) * (cols.stream().mapToInt(col -> featureColumnHeight(field, col)).sum())
			+ (-1) * (cols.stream().filter(col -> {return col <= 8;}).mapToInt(col -> featureAbsoluteAdjColumnHeightDiff(field, col)).sum())
			+ (-1) * featureMaxColumnHeight(field)
			+ (-1) * featureNumOfHoles(field);
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
