import java.util.*;
import java.util.stream.*;

public class NoVisualPlayerSkeleton {
    public static void main(String[] args) {
        PlayerSkeleton p = new PlayerSkeleton(Arrays.asList(args).stream().mapToDouble(weightStr -> Double.parseDouble(weightStr)).boxed().collect(Collectors.toCollection(ArrayList::new)));
        NoVisualState s = new NoVisualState();
        while(!s.hasLost()) s.makeMove(p.pickMoveImpl(s.getField(), s.legalMoves(), s.getTop(), s.getpOrients(), s.getpWidth(), s.getpHeight(), s.getpBottom(), s.getpTop(), s.getNextPiece()));
        System.out.println(s.getRowsCleared());
    }
}
