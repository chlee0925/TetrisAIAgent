public class NoVisualPlayerSkeleton {
    public static void main(String[] args) {
        double[] weightVectors = new double[]{ 20.0 // Reward
                                            , 1.0, 1.0, 1.0, 1.0, 1.0
                                            , 1.0, 1.0, 1.0, 1.0, 1.0
                                            , 1.0, 1.0, 1.0, 1.0, 1.0
                                            , 1.0, 1.0, 1.0, 1.0, 1.0
                                            , 5.0};
        if (args.length > 0){
            for(int argIndex = 0; argIndex < args.length; argIndex++) {
                weightVectors[argIndex] = Double.parseDouble(args[argIndex]) ;
                // System.out.print("|r:" + weightVectors[argIndex]);
            }
        }
        PlayerSkeleton p = new PlayerSkeleton(weightVectors);
        NoVisualState s = new NoVisualState();
        while(!s.hasLost()) s.makeMove(p.pickMoveImpl(s.getField(), s.legalMoves(), s.getTop(), s.getpOrients(), s.getpWidth(), s.getpHeight(), s.getpBottom(), s.getpTop(), s.getNextPiece()));
        System.out.println(s.getRowsCleared());
    }
}
