import java.util.*;
import java.util.stream.*;
import java.util.concurrent.*;

public class NoVisualPlayerSkeleton {

    public static final int TIMES = 10;
    public static final int THREAD_POOL_SIZE = 10;
    public static long turnLimiter = 0; // limits the max turns per game when positive. (x <= 0) for being unlimited.
    public static int rowsCleared = 0; // do not modify directly.
    public static boolean threading = true;
    public synchronized static void addRowsCleared(int addRowsCleared) {
        rowsCleared+=addRowsCleared;
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            try {
                int threading_option = Integer.parseInt(args[0]);
                if (threading_option == 0) {
                    threading = false;
                }
            } catch (Exception e) {
                threading = true;
            }
            try { // second parameter is a turn limiter
                turnLimiter = Long.parseLong(args[1]);
            } catch (NumberFormatException nfe) { // given number may have overflowed
                turnLimiter = Long.MAX_VALUE;
            }
            args = Arrays.copyOfRange(args, 2, args.length);
        }

        if (threading) {
            multiThread(args);
        } else {
            System.out.println(singleRun(args));
        }
    }

    public static int singleRun(String[] args) {
        PlayerSkeleton p = new PlayerSkeleton(Arrays.asList(args).stream().mapToDouble(weightStr -> Double.parseDouble(weightStr)).boxed().collect(Collectors.toCollection(ArrayList::new)));
        NoVisualState s = new NoVisualState();
        while(!(s.hasLost() || (turnLimiter > 0 && s.getTurnNumber() >= turnLimiter))) {
            s.makeMove(p.pickMoveImpl(s.getField(), s.legalMoves(), s.getTop(), s.getpOrients(), s.getpWidth(), s.getpHeight(), s.getpBottom(), s.getpTop(), s.getNextPiece()));
        }
        return s.getRowsCleared();
    }

    public static void multiThread(String[] args) throws Exception {
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
            addRowsCleared(singleRun(this.args));
        }

    }
}
