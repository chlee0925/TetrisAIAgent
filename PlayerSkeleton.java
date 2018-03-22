
public class PlayerSkeleton {

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
		return 0;
	}
}
