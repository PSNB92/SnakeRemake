package org.psnbtech;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JFrame;

/**
 * The {@code SnakeGame} class is responsible for handling much of the game's logic.
 * @author Brendan Jones
 *
 */
public class SnakeGame extends JFrame {
		
	/**
	 * The Serial Version UID.
	 */
	private static final long serialVersionUID = 6678292058307426314L;

	/**
	 * The number of milliseconds that should pass between each frame.
	 */
	private static final long FRAME_TIME = 1000L / 50L;
	
	/**
	 * The minimum length of the snake. This allows the snake to grow
	 * right when the game starts, so that we're not just a head moving
	 * around on the board.
	 */
	private static final int MIN_SNAKE_LENGTH = 5;
	
	/**
	 * The maximum number of directions that we can have polled in the
	 * direction list.
	 */
	private static final int MAX_DIRECTIONS = 3;
	
	/**
	 * The BoardPanel instance.
	 */
	private BoardPanel board;
	
	/**
	 * The SidePanel instance.
	 */
	private SidePanel side;
	
	/**
	 * The random number generator (used for spawning fruits).
	 */
	private Random random;
	
	/**
	 * The Clock instance for handling the game logic.
	 */
	private Clock logicTimer;
	
	/**
	 * Whether or not we're running a new game.
	 */
	private boolean isNewGame;
		
	/**
	 * Whether or not the game is over.
	 */
	private boolean isGameOver;
	
	/**	
	 * Whether or not the game is paused.
	 */
	private boolean isPaused;
	
	/**
	 * The list that contains the points for the snake.
	 */
	private LinkedList<Point> snake;
	
	/**
	 * The list that contains the queued directions.
	 */
	private LinkedList<Direction> directions;
	
	/**
	 * The current score.
	 */
	private int score;
	
	/**
	 * The number of fruits that we've eaten.
	 */
	private int fruitsEaten;
	
	/**
	 * The number of points that the next fruit will award us.
	 */
	private int nextFruitScore;
	
	/**
	 * Creates a new SnakeGame instance. Creates a new window,
	 * and sets up the controller input.
	 */
	private SnakeGame() {
		super("Snake Remake");
		setLayout(new BorderLayout());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
				
		/*
		 * Initialize the game's panels and add them to the window.
		 */
		this.board = new BoardPanel(this);
		this.side = new SidePanel(this);
		
		add(board, BorderLayout.CENTER);
		add(side, BorderLayout.EAST);
		
		/*
		 * Adds a new key listener to the frame to process input. 
		 */
		addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				switch(e.getKeyCode()) {

				/*
				 * If the game is not paused, and the game is not over...
				 * 
				 * Ensure that the direction list is not full, and that the most
				 * recent direction is adjacent to North before adding the
				 * direction to the list.
				 */
				case KeyEvent.VK_W:
				case KeyEvent.VK_UP:
					if(!isPaused && !isGameOver) {
						if(directions.size() < MAX_DIRECTIONS) {
							Direction last = directions.peekLast();
							if(last != Direction.South && last != Direction.North) {
								directions.addLast(Direction.North);
							}
						}
					}
					break;

				/*
				 * If the game is not paused, and the game is not over...
				 * 
				 * Ensure that the direction list is not full, and that the most
				 * recent direction is adjacent to South before adding the
				 * direction to the list.
				 */	
				case KeyEvent.VK_S:
				case KeyEvent.VK_DOWN:
					if(!isPaused && !isGameOver) {
						if(directions.size() < MAX_DIRECTIONS) {
							Direction last = directions.peekLast();
							if(last != Direction.North && last != Direction.South) {
								directions.addLast(Direction.South);
							}
						}
					}
					break;
				
				/*
				 * If the game is not paused, and the game is not over...
				 * 
				 * Ensure that the direction list is not full, and that the most
				 * recent direction is adjacent to West before adding the
				 * direction to the list.
				 */						
				case KeyEvent.VK_A:
				case KeyEvent.VK_LEFT:
					if(!isPaused && !isGameOver) {
						if(directions.size() < MAX_DIRECTIONS) {
							Direction last = directions.peekLast();
							if(last != Direction.East && last != Direction.West) {
								directions.addLast(Direction.West);
							}
						}
					}
					break;
			
				/*
				 * If the game is not paused, and the game is not over...
				 * 
				 * Ensure that the direction list is not full, and that the most
				 * recent direction is adjacent to East before adding the
				 * direction to the list.
				 */		
				case KeyEvent.VK_D:
				case KeyEvent.VK_RIGHT:
					if(!isPaused && !isGameOver) {
						if(directions.size() < MAX_DIRECTIONS) {
							Direction last = directions.peekLast();
							if(last != Direction.West && last != Direction.East) {
								directions.addLast(Direction.East);
							}
						}
					}
					break;
				
				/*
				 * If the game is not over, toggle the paused flag and update
				 * the logicTimer's pause flag accordingly.
				 */
				case KeyEvent.VK_P:
					if(!isGameOver) {
						isPaused = !isPaused;
						logicTimer.setPaused(isPaused);
					}
					break;
				
				/*
				 * Reset the game if one is not currently in progress.
				 */
				case KeyEvent.VK_ENTER:
					if(isNewGame || isGameOver) {
						resetGame();
					}
					break;
				}
			}
			
		});
		
		/*
		 * Resize the window to the appropriate size, center it on the
		 * screen and display it.
		 */
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	/**
	 * Starts the game running.
	 */
	private void startGame() {
		/*
		 * Initialize everything we're going to be using.
		 */
		this.random = new Random();
		this.snake = new LinkedList<>();
		this.directions = new LinkedList<>();
		this.logicTimer = new Clock(9.0f);
		this.isNewGame = true;
		
		//Set the timer to paused initially.
		logicTimer.setPaused(true);

		/*
		 * This is the game loop. It will update and render the game and will
		 * continue to run until the game window is closed.
		 */
		while(true) {
			//Get the current frame's start time.
			long start = System.nanoTime();
			
			//Update the logic timer.
			logicTimer.update();
			
			/*
			 * If a cycle has elapsed on the logic timer, then update the game.
			 */
			if(logicTimer.hasElapsedCycle()) {
				updateGame();
			}
			
			//Repaint the board and side panel with the new content.
			board.repaint();
			side.repaint();
			
			/*
			 * Calculate the delta time between since the start of the frame
			 * and sleep for the excess time to cap the frame rate. While not
			 * incredibly accurate, it is sufficient for our purposes.
			 */
			long delta = (System.nanoTime() - start) / 1000000L;
			if(delta < FRAME_TIME) {
				try {
					Thread.sleep(FRAME_TIME - delta);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Updates the game's logic.
	 */
	private void updateGame() {
		/*
		 * Gets the type of tile that the head of the snake collided with. If 
		 * the snake hit a wall, SnakeBody will be returned, as both conditions
		 * are handled identically.
		 */
		TileType collision = updateSnake();
		
		/*
		 * Here we handle the different possible collisions.
		 * 
		 * Fruit: If we collided with a fruit, we increment the number of
		 * fruits that we've eaten, update the score, and spawn a new fruit.
		 * 
		 * SnakeBody: If we collided with our tail (or a wall), we flag that
		 * the game is over and pause the game.
		 * 
		 * If no collision occurred, we simply decrement the number of points
		 * that the next fruit will give us if it's high enough. This adds a
		 * bit of skill to the game as collecting fruits more quickly will
		 * yield a higher score.
		 */
		if(collision == TileType.Fruit) {
			fruitsEaten++;
			score += nextFruitScore;
			spawnFruit();
		} else if(collision == TileType.SnakeBody) {
			isGameOver = true;
			logicTimer.setPaused(true);
		} else if(nextFruitScore > 10) {
			nextFruitScore--;
		}
	}
	
	/**
	 * Updates the snake's position and size.
	 * @return Tile tile that the head moved into.
	 */
	private TileType updateSnake() {

		/*
		 * Here we peek at the next direction rather than polling it. While
		 * not game breaking, polling the direction here causes a small bug
		 * where the snake's direction will change after a game over (though
		 * it will not move).
		 */
		Direction direction = directions.peekFirst();
				
		/*
		 * Here we calculate the new point that the snake's head will be at
		 * after the update.
		 */		
		Point head = new Point(snake.peekFirst());
		switch(direction) {
		case North:
			head.y--;
			break;
			
		case South:
			head.y++;
			break;
			
		case West:
			head.x--;
			break;
			
		case East:
			head.x++;
			break;
		}
		
		/*
		 * If the snake has moved out of bounds ('hit' a wall), we can just
		 * return that it's collided with itself, as both cases are handled
		 * identically.
		 */
		if(head.x < 0 || head.x >= BoardPanel.COL_COUNT || head.y < 0 || head.y >= BoardPanel.ROW_COUNT) {
			return TileType.SnakeBody; //Pretend we collided with our body.
		}
		
		/*
		 * Here we get the tile that was located at the new head position and
		 * remove the tail from of the snake and the board if the snake is
		 * long enough, and the tile it moved onto is not a fruit.
		 * 
		 * If the tail was removed, we need to retrieve the old tile again
		 * incase the tile we hit was the tail piece that was just removed
		 * to prevent a false game over.
		 */
		TileType old = board.getTile(head.x, head.y);
		if(old != TileType.Fruit && snake.size() > MIN_SNAKE_LENGTH) {
			Point tail = snake.removeLast();
			board.setTile(tail, null);
			old = board.getTile(head.x, head.y);
		}
		
		/*
		 * Update the snake's position on the board if we didn't collide with
		 * our tail:
		 * 
		 * 1. Set the old head position to a body tile.
		 * 2. Add the new head to the snake.
		 * 3. Set the new head position to a head tile.
		 * 
		 * If more than one direction is in the queue, poll it to read new
		 * input.
		 */
		if(old != TileType.SnakeBody) {
			board.setTile(snake.peekFirst(), TileType.SnakeBody);
			snake.push(head);
			board.setTile(head, TileType.SnakeHead);
			if(directions.size() > 1) {
				directions.poll();
			}
		}
				
		return old;
	}
	
	/**
	 * Resets the game's variables to their default states and starts a new game.
	 */
	private void resetGame() {
		/*
		 * Reset the score statistics. (Note that nextFruitPoints is reset in
		 * the spawnFruit function later on).
		 */
		this.score = 0;
		this.fruitsEaten = 0;
		
		/*
		 * Reset both the new game and game over flags.
		 */
		this.isNewGame = false;
		this.isGameOver = false;
		
		/*
		 * Create the head at the center of the board.
		 */
		Point head = new Point(BoardPanel.COL_COUNT / 2, BoardPanel.ROW_COUNT / 2);

		/*
		 * Clear the snake list and add the head.
		 */
		snake.clear();
		snake.add(head);
		
		/*
		 * Clear the board and add the head.
		 */
		board.clearBoard();
		board.setTile(head, TileType.SnakeHead);
		
		/*
		 * Clear the directions and add north as the
		 * default direction.
		 */
		directions.clear();
		directions.add(Direction.North);
		
		/*
		 * Reset the logic timer.
		 */
		logicTimer.reset();
		
		/*
		 * Spawn a new fruit.
		 */
		spawnFruit();
	}
	
	/**
	 * Gets the flag that indicates whether or not we're playing a new game.
	 * @return The new game flag.
	 */
	public boolean isNewGame() {
		return isNewGame;
	}
	
	/**
	 * Gets the flag that indicates whether or not the game is over.
	 * @return The game over flag.
	 */
	public boolean isGameOver() {
		return isGameOver;
	}
	
	/**
	 * Gets the flag that indicates whether or not the game is paused.
	 * @return The paused flag.
	 */
	public boolean isPaused() {
		return isPaused;
	}
	
	/**
	 * Spawns a new fruit onto the board.
	 */
	private void spawnFruit() {
		//Reset the score for this fruit to 100.
		this.nextFruitScore = 100;

		/*
		 * Get a random index based on the number of free spaces left on the board.
		 */
		int index = random.nextInt(BoardPanel.COL_COUNT * BoardPanel.ROW_COUNT - snake.size());
		
		/*
		 * While we could just as easily choose a random index on the board
		 * and check it if it's free until we find an empty one, that method
		 * tends to hang if the snake becomes very large.
		 * 
		 * This method simply loops through until it finds the nth free index
		 * and selects uses that. This means that the game will be able to
		 * locate an index at a relatively constant rate regardless of the
		 * size of the snake.
		 */
		int freeFound = -1;
		for(int x = 0; x < BoardPanel.COL_COUNT; x++) {
			for(int y = 0; y < BoardPanel.ROW_COUNT; y++) {
				TileType type = board.getTile(x, y);
				if(type == null || type == TileType.Fruit) {
					if(++freeFound == index) {
						board.setTile(x, y, TileType.Fruit);
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Gets the current score.
	 * @return The score.
	 */
	public int getScore() {
		return score;
	}
	
	/**
	 * Gets the number of fruits eaten.
	 * @return The fruits eaten.
	 */
	public int getFruitsEaten() {
		return fruitsEaten;
	}
	
	/**
	 * Gets the next fruit score.
	 * @return The next fruit score.
	 */
	public int getNextFruitScore() {
		return nextFruitScore;
	}
	
	/**
	 * Gets the current direction of the snake.
	 * @return The current direction.
	 */
	public Direction getDirection() {
		return directions.peek();
	}
	
	/**
	 * Entry point of the program.
	 * @param args Unused.
	 */
	public static void main(String[] args) {
		SnakeGame snake = new SnakeGame();
		snake.startGame();
	}

}
