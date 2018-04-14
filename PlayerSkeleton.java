import java.util.*;
import java.util.stream.*;

public class PlayerSkeleton {
	public static final int COLS = 10;
	public static final int ROWS = 21;
	public static final int N_PIECES = 7;
    public double[] weightVectors = new double[]{ 
                                                // Reward
                                                20.0,
                                                // Features
                                                1.0, // 1 - Max Height
                                                1.0, // 2 - Num Of Holes
                                                1.0, // 3 - Landing Height
                                                1.0, // 4 - Cell Transition
                                                1.0, // 5 - Height Diff Sum
                                                1.0, // 6 - Mean Column Height
                                                1.0, // 7 - Depth Of Wells
                                                1.0, // 8 - Height Weighted Cells
                                                1.0, // 9 - Num Of Full Cells
                                                1.0, // 10 - Row Breaks
                                                1.0  // 11 - Col Breaks
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
            
            // Create temporary field to simulate a move
            int[][] tempField = new int[field.length][];
            for(int i = 0; i < tempField.length; i++) {
                tempField[i] = Arrays.copyOf(field[i], field[i].length);
            }
            // Create temporary top to simulate a move
			int[] tempTop = Arrays.copyOf(top, top.length);

			///////////////
			///Make Move///
			///////////////

			// Find height where the piece is placed at
			int height = tempTop[slot] - pBottom[nextPiece][orient][0];
			// For each column beyond the first in the piece
			for (int c = 1; c < pWidth[nextPiece][orient]; c++) {
				height = Math.max(height, tempTop[slot + c] - pBottom[nextPiece][orient][c]);
			}

			// If the game is going to end, try the next move
			if (height + pHeight[nextPiece][orient] >= ROWS) {
				continue;
			}

			// for each column in the piece - fill in the appropriate blocks
			for (int i = 0; i < pWidth[nextPiece][orient]; i++) {
				//from bottom to top of brick
				for (int h = height + pBottom[nextPiece][orient][i]; h < height + pTop[nextPiece][orient][i]; h++) {
					tempField[h][i + slot] = Integer.MAX_VALUE;
				}
			}
			
			//adjust tempoaray top
			for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
				tempTop[slot+c]=height+pTop[nextPiece][orient][c];
            }

            int rowsCleared = 0;
            
            //check for full rows - starting at the top
            for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
                //check all columns in the row
                boolean full = true;
                for(int c = 0; c < COLS; c++) {
                    if(tempField[r][c] == 0) {
                        full = false;
                        break;
                    }
                }
                //if the row was full - remove it and slide above stuff down
                if(full) {
                    rowsCleared++;
                    //for each column
                    for(int c = 0; c < COLS; c++) {
                        //slide down all bricks
                        for(int i = r; i < tempTop[c]; i++) {
                            tempField[i][c] = tempField[i+1][c];
                        }
                        //lower the top
                        tempTop[c]--;
                        while(tempTop[c]>=1 && tempField[tempTop[c]-1][c]==0) tempTop[c]--;
                    }
                }
            }

			/////////////////////////////////
			///Run the evaluation function///
			/////////////////////////////////

			// Calculate the evaluation value
			double evaluationValue = evaluationFunction(tempField, tempTop, height, rowsCleared);
			if (evaluationValue > currentBest) {
				currentBest = evaluationValue;
				moveDecision = moveIndex;
			}
		}

		return moveDecision;
	}

	public double evaluationFunction(int[][] field, int[] top, int landingHeight, int rowsCleared) {
		final int featureColumnHeightIndex = 1;
		final int featureAbsoluteAdjColumnHeightDiffIndex = 11;

		return

            // INDEX 0 - REWARD
            (weightVectors[0]) * rowsCleared

            // FEATURE 1 - Max Height
            - (weightVectors[1]) * featureMaxColumnHeight(top)

            // FEATURE 2 - Num Of Holes
            - (weightVectors[2]) * featureNumOfHoles(field, top)

            // FEATURE 3 - Landing Height
            - (weightVectors[3]) * landingHeight

            // FEATURE 4 - Cell Transition
            - (weightVectors[4]) * featureCellTransitions(field, top)

            // FEATURE 5 - Height Diff Sum
            - (weightVectors[5]) * featureHeightDiffSum(top)

            // FEATURE 6 - Mean Column Height
            - (weightVectors[6]) * featureMeanColumnHeight(top)

            // FEATURE 7 - Depth Of Wells
            - (weightVectors[7]) * featureDepthOfWells(top)

            // FEATURE 8 - Height Weighted Cells
            - (weightVectors[8]) * featureHeightWeightedCells(field, top)

            // FEATURE 9 - Num Of Full Cells
            - (weightVectors[9]) * featureNumOfFullCells(field, top)

            // FEATURE 10 - Row Breaks
            - (weightVectors[10]) * featureRowBreaks(field, top)

            // FEATURE 11 - Col Breaks
            - (weightVectors[11]) * featureColumnBreaks(field, top);

    }

	///////////////////////////////////////
	///////////		FEATURES	///////////
	///////////////////////////////////////

	/**
	 * feature removed
	 * Column Height
	 */
	public int featureColumnHeight(int[] top, int col) {
		return top[col];
	}

	/**
	 * feature removed
	 * Absolute height difference between (col) and (col+1) columns.
	 */
	public int featureAbsoluteAdjColumnHeightDiff(int[] top, int col) {
		return Math.abs(top[col] - top[col+1]);
	}

	/**
	 * FEATURE 1 - Max Height
	 * Maximum height across all columns
	 */
	public int featureMaxColumnHeight(int[] top) {
		return getMaxColHeight(top);
	}

	/**
	 * FEATURE 2 - Num Of Holes
	 * The number of holes in the wall: the number of empty positions of
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
     * FEATURE 4 - Cell Transition
     * The number of empty cells/borders touching the edges of full cells.
     */
    public int featureCellTransitions(int[][] field, int[] top) {
        int numOfCellTrns = 0;

        int[] rowTrans = {1, -1, 0, 0};
        int[] colTrans = {0, 0, 1, -1};

        int maxHeight = getMaxColHeight(top);
        for (int col = 0; col < top.length; col++) {
            for (int row = 0; row <= maxHeight; row++) {
                if (!isPositionValid(row, col)) continue;
                if (field[row][col] != 0) continue; // only interested in empty cell
                
                boolean isCellTransition = false;
                for (int i = 0; i < 4; i++) { // four neighbours
                    int neighRow = row + rowTrans[i];
                    int neighCol = col + colTrans[i];

                    if (isPositionValid(neighRow, neighCol) && field[neighRow][neighCol] != 0) {
                       isCellTransition = true;
                       break;
                    }
                }

                if (isCellTransition) numOfCellTrns++;
            }
        }

        return numOfCellTrns;
    }

    /**
     * FEATURE 5 - Height Diff Sum
     * Sum of the height differences between adjacent columns.
     */
    public int featureHeightDiffSum(int[] top) {
        int heightDiffSum = 0;
        for (int col = 0; col < top.length - 1; col++) {
            heightDiffSum += Math.abs(top[col] - top[col + 1]);
        }
        return heightDiffSum;
    }

    /**
     * FEATURE 6 - Mean Column Height
     * Average column heights.
     */
    public double featureMeanColumnHeight(int[] top) {
        return Arrays.stream(top).mapToDouble(height -> (double) height).average().getAsDouble();
    }
    
    /**
     * FEATURE 7 - Depth Of Wells
     * Number of blocks in wells: min depth 1, max width 1)
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
    
    /**
     * FEATURE 8 - Height Weighted Cells
     * Sum of full cells weighted by their row number
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
     * FEATURE 9 - Num Of Full Cells
     * Number of occupied cells on the board
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

    /**
     * FEATURE 10 - Row BreaksRow Breaks
     * Number of transitions between a filled and empty cell in each row
     */
    public int featureRowBreaks(int[][] field, int[] top) {
        int rowTransition = 0;
        int maxHeight = getMaxColHeight(top);
        for (int row = 0; row < maxHeight; row++) {
            int previousState = field[row][0];
            for (int col = 1; col < COLS; col++) {
                if ((field[row][col] != 0) != (previousState != 0)) {
                    rowTransition++;
                }
                previousState = field[row][col];
            }
        }
        return rowTransition;
    }

    /**
     * FEATURE 11 - Col Breaks
     * Number of transitions between a filled and empty cell in each column
     */
    public int featureColumnBreaks(int[][] field, int[] top) {
        int colTransition = 0;
        for (int col = 0; col < COLS; col++) {
            int previousState = field[0][col];
            for (int row = 1; row < top[col]; row++) {
                if ((field[row][col] != 0) != (previousState != 0)) {
                    colTransition++;
                }
                previousState = field[row][col];
            }
        }
        return colTransition;
    }

    ///////////////////////////////////////////////
    ///////////  AUXILIARY FUNCTIONS  /////////////
    ///////////////////////////////////////////////

	public int getMinColHeight(int[] top) {
		int minHeight = Integer.MAX_VALUE;
		for (int height : top) {
			if (minHeight > height) {
				minHeight = height;
			}
		}
		return minHeight;
    }

    public int getMaxColHeight(int[] top) {
        int maxHeight = 0;
        for (int height : top) {
            if (maxHeight < height) {
                maxHeight = height;
            }
        }
        return maxHeight;
    }
    
    /**
     * @param row row index
     * @param col col index
     */
    public boolean isPositionValid(int row, int col) {
        return (row >= 0) && (row < ROWS - 1) && (col >= 0) && (col < COLS);
    }

}
