# Tetris AI player

[![CircleCI](https://circleci.com/gh/chlee0925/TetrisAIAgent.svg?style=svg)](https://circleci.com/gh/chlee0925/TetrisAIAgent)

## How to build / run:
```
./run.sh		# With visualization

OR

./run.sh --novisual	# Without visualization
```

## Files in the project:
	State - tetris simulation
	TFrame - frame that draws the board
	TLabel - drawing library
	PlayerSkeleton - setup for implementing a player
	NoVisualState / NoVisualPlayerSkeleton - non-visual versions for fast execution
	
	
## State:
This is the tetris simulation.  It keeps track of the tetris state and allows you to 
make moves.  

`field`: The board state is stored in `field` (a 2D int array) and is accessed by `getField()`.
* Zero-value: empty square.
* Non-zero value: filled square. The number refers to the turn on which that square was placed.  

`nextPiece`: (accessed by `getNextPiece()`) contains the ID (0-6) of the piece you are about to play.
* Tetris piece information (naming according to http://tetris.wikia.com/wiki/Tetromino)
```
ID and Orientation index according to the program configuration

ID 0 (name 'O')

	Orientation
	0

	XX	
	XX

ID 1 (name 'I')

	Orientation
	0		1

	XXXX		X
			X
			X
			X

ID 2 (name 'L')

	Orientation
	0	1	2	3

	X	XXX	XX	  X
	X	X	 X	XXX
	XX		 X

ID 3 (name "J")

	Orientation
	0	1	2	3

	 X	X	XX	XXX
	 X	XXX	X	  X
	XX		X

ID 4 (name "T")

	Orientation
	0	1	2	3

	X	XXX	 X	 X
	XX	 X	XX	XXX
	X		 X

ID 5 (name "S")

	Orientation
	0	1

	 XX	X
	XX	XX
		 X

ID 6 (name "Z")

	Orientation
	0	1

	XX	 X
	 XX	XX
		X
```

### Making moves
* `slot`: (int) the leftmost column on which the next piece is to be placed
* `orient`: (int) the orientation index of the next piece
* `legalMoves()`: returns (nx2) int array containing the n legal moves.
* `makeMove()`: Making a move using the next piece. Argument options:
	* `orient` and `slot`
	* an int array {`orient`, `slot`}
	* row index of the (nx2) array returned by `legalMoves()` call

`cleared` accessed by `getRowsCleared()`: the number of lines cleared

`draw()`: draws the board.

`drawNext()`: draws the next piece above the board

`clearNext()`: clears the drawing of the next piece so it can be subsequently drawn in the updated slot/orientation using `drawNext()`

## TFrame:
This extends JFrame and is instantiated to draw a state.
It can save the current drawing to a .png file.
The main function allows you to play a game manually using the arrow keys.

## TLabel:
This is a drawing library.

## PlayerSkeleton:
The main function plays a game automatically (with visualization).
