import java.util.*;
import java.util.stream.*;
import java.util.concurrent.*;

public class NoVisualPlayerSkeleton {

    public static final int TIMES = 10;
    public static final int THREAD_POOL_SIZE = 10;
    public static long turnLimiter = 0; // limits the max turns per game when positive. (x <= 0) for being unlimited.
    public static int rowsCleared = 0; // do not modify directly.

    public synchronized static void addRowsCleared(int addRowsCleared) {
        rowsCleared+=addRowsCleared;
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) { // first parameter is a turn limiter
            try {
                turnLimiter = Long.parseLong(args[0]);
            } catch (NumberFormatException nfe) { // given number may have overflowed
                turnLimiter = Long.MAX_VALUE;
            }
            args = Arrays.copyOfRange(args, 1, args.length);
        }

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
            while(!(s.hasLost() || (turnLimiter > 0 && s.getTurnNumber() >= turnLimiter))) {
                s.makeMove(p.pickMoveImpl(s.getField(), s.legalMoves(), s.getTop(), s.getpOrients(), s.getpWidth(), s.getpHeight(), s.getpBottom(), s.getpTop(), s.getNextPiece()));
            }
            addRowsCleared(s.getRowsCleared());
        }

    }
}
