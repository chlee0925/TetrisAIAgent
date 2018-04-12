import java.util.*;
import java.util.stream.*;
import java.util.concurrent.*;

public class NoVisualPlayerSkeleton {

    public static final int TIMES = 10;
    public static final int THREAD_POOL_SIZE = 10;
    public static int rowsCleared = 0; // do not modify directly.

    public synchronized static void addRowsCleared(int addRowsCleared) {
        rowsCleared+=addRowsCleared;
    }

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        for (int i = 0; i < TIMES; i++) {
            executor.execute(new Worker(args));
        }
    
        executor.shutdown(); // stop accepting new threads.        
        executor.awaitTermination(1, TimeUnit.DAYS); // Wait until all threads are done

        System.out.println(rowsCleared/TIMES);
    }

    static class Worker implements Runnable {

        public String[] args;
        
        public Worker(String[] args) { this.args = args; }

        @Override
        public void run() {
            PlayerSkeleton p = new PlayerSkeleton(Arrays.asList(this.args).stream().mapToDouble(weightStr -> Double.parseDouble(weightStr)).boxed().collect(Collectors.toCollection(ArrayList::new)));
            NoVisualState s = new NoVisualState();
            while(!s.hasLost()) s.makeMove(p.pickMoveImpl(s.getField(), s.legalMoves(), s.getTop(), s.getpOrients(), s.getpWidth(), s.getpHeight(), s.getpBottom(), s.getpTop(), s.getNextPiece()));
            addRowsCleared(s.getRowsCleared());
        }

    }
}
