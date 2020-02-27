package de.rondiplomatico.nds;

import java.awt.Point;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Utility class for flood fill algorithm applied to marked tiles deduced from polygon points.
 * 
 * @author Andreas Hessenthaler
 * @since 13.02.2020
 */
public class floodFill {
	int[][] ff;
	int minX 					= Integer.MAX_VALUE;
	int maxX 					= Integer.MIN_VALUE;
	int minY 					= Integer.MAX_VALUE;
	int maxY 					= Integer.MIN_VALUE;
	int dimX 					= -1;
	int dimY 					= -1;
	int bg 						= 0;
	int bound 					= 1;
	int fill 					= bound;
	int startAtX 				= 0;
	int startAtY 				= 0;
	
	public floodFill(int[][] f, int a, int b, int c, int d) {
		ff 						= f;
		minX 					= a;
		maxX 					= b;
		minY 					= c;
		maxY 					= d;
		dimX 					= maxX - minX + 1;
		dimY 					= maxY - minY + 1;
		setStartingPoint();
	}
	
	/*
	 * Four-way recursive flood fill algorithm
	 * 
	 * @param x
	 * 			the tile x coordinate
	 * @param y
	 * 			the tile y coordinate
	 */
	public void recursiveFloodFill(int x, int y) {
		if ((x < 0) || (y < 0) || (x > dimX-1 || (y > dimY-1))) {
			return;
		}
		if (ff[x][y] == bg) {
			ff[x][y] 			= fill;
			recursiveFloodFill(x+1, y  ); // right
			recursiveFloodFill(x,   y+1); // top
			recursiveFloodFill(x-1, y  ); // left
			recursiveFloodFill(x,   y-1); // bottom
		}
		return;
	}

	/*
	 * Eight-way recursive flood fill algorithm
	 * 
	 * @param x
	 * 			the tile x coordinate
	 * @param y
	 * 			the tile y coordinate
	 */
	public void recursiveFloodFill8(int x, int y) {
		if ((x < 0) || (y < 0) || (x > dimX-1 || (y > dimY-1))) {
			return;
		}
		if (ff[x][y] == bg) {
			ff[x][y] 			= fill;
			recursiveFloodFill(x+1, y  ); // right
			recursiveFloodFill(x,   y+1); // top
			recursiveFloodFill(x-1, y  ); // left
			recursiveFloodFill(x,   y-1); // bottom
			recursiveFloodFill(x+1, y+1); // top right
			recursiveFloodFill(x-1, y+1); // top left
			recursiveFloodFill(x+1, y-1); // bottom right
			recursiveFloodFill(x-1, y-1); // bottom left
		}
		return;
	}

	/*
	 * Iterative four-way flood fill algorithm - avoids stack overflow issues
	 * 
	 */
	public void iterativeFloodFill() {
		Queue<Point> q 			= new LinkedList<Point>();
		q.add(new Point(startAtX, startAtY));
		while (!q.isEmpty()) {
			Point p 			= q.remove();
			if ((p.x < 0) || (p.y < 0) || (p.x > dimX-1 || (p.y > dimY-1))) {
				continue;
			}
			if (ff[p.x][p.y] == bg) {
				ff[p.x][p.y] 	= fill;
				q.add(new Point(p.x+1, p.y  )); // right
				q.add(new Point(p.x,   p.y+1)); // top
				q.add(new Point(p.x-1, p.y  )); // left
				q.add(new Point(p.x,   p.y-1)); // bottom
			}
		}
	}

	/*
	 * Iterative eight-way flood fill algorithm - avoids stack overflow issues
	 * 
	 */
	public void iterativeFloodFill8() {
		Queue<Point> q 			= new LinkedList<Point>();
		q.add(new Point(startAtX, startAtY));
		while (!q.isEmpty()) {
			Point p 			= q.remove();
			if ((p.x < 0) || (p.y < 0) || (p.x > dimX-1 || (p.y > dimY-1))) {
				continue;
			}
			if (ff[p.x][p.y] == bg) {
				ff[p.x][p.y] 	= fill;
				q.add(new Point(p.x+1, p.y  )); // right
				q.add(new Point(p.x,   p.y+1)); // top
				q.add(new Point(p.x-1, p.y  )); // left
				q.add(new Point(p.x,   p.y-1)); // bottom
				q.add(new Point(p.x+1, p.y+1)); // top right
				q.add(new Point(p.x-1, p.y+1)); // top left
				q.add(new Point(p.x+1, p.y-1)); // bottom right
				q.add(new Point(p.x-1, p.y-1)); // bottom left
			}
		}
	}
	
	/*
	 * Safety net for filling left over lines after applying standard flood fill algorithm
	 * 
	 */
	public void fillHoles() {
		// first, find vertical lines
		boolean vline 					= false;
		boolean vlineEnded				= false;
		int currStartY					= -1;
		int currStopY					= -1;
		for (int col = 1; col < dimY-1; col++) {
			vline 						= false;
			vlineEnded 					= false;
			for (int row = 1; row < dimX-1; row++) {
				// minimum requirement: left and right neighbors are not background
				if ((ff[row][col-1] != bg) && (ff[row][col+1] != bg)) {
					if ((ff[row][col] == bg)
						&& (ff[row-1][col] != bg)) {
						// beginning of vertical line
						currStartY 		= row;
						vline 			= true;
					} else if (vline && (ff[row][col] == bg)) {
						// vertical line continues
					} else if (vline && (ff[row][col] != bg)) {
						// end of vertical line
						currStopY 		= row;
						vlineEnded 		= true;
					}
				} else if (vline && ((ff[row][col+1] != bg) || (ff[row][col-1] != bg)) && (ff[row][col] != bg)) {
					// end of horizontal line
					currStopY 		= row;
					vlineEnded 		= true;
				} else {
					vline = false;
					vlineEnded = false;
				}
				if (vline && vlineEnded) {
					// fill line and reset booleans
					System.out.println(">>>>>>INFO: Filling vertical line.");
					for (int idx = currStartY; idx < currStopY; idx++) {
						ff[idx][col]	= fill;
					}
					vline 				= false;
					vlineEnded 			= false;
				}
			}
		}
		// second, find horizontal lines
		boolean hline 					= false;
		boolean hlineEnded				= false;
		for (int row = 1; row < dimX-1; row++) {
			hline 						= false;
			hlineEnded 					= false;
			for (int col = 1; col < dimY-1; col++) {
				// minimum requirement: top and bottom neighbors are not background
				if ((ff[row+1][col] != bg) && (ff[row-1][col] != bg)) {
					if ((ff[row][col] == bg)
						&& (ff[row][col-1] != bg)) {
						// beginning of horizontal line
						currStartY 		= col;
						hline 			= true;
					} else if (hline && (ff[row][col] == bg)) {
						// vertical line continues
					} else if (hline && (ff[row][col] != bg)) {
						// end of vertical line
						currStopY 		= col;
						hlineEnded 		= true;
					}
				} else if (hline && ((ff[row+1][col] != bg) || (ff[row-1][col] != bg)) && (ff[row][col] != bg)) {
					// end of vertical line
					currStopY 		= col;
					hlineEnded 		= true;
				} else {
					hline = false;
					hlineEnded = false;
				}
				if (hline && hlineEnded) {
					// fill line and reset booleans
					System.out.println(">>>>>>INFO: Filling horizontal line.");
					for (int idx = currStartY; idx < currStopY; idx++) {
						ff[row][idx]	= fill;
					}
					hline 				= false;
					hlineEnded 			= false;
				}
			}
		}
		
		return;
	}
	
	/*
	 * Set initial point for flood fill algorithm
	 * 
	 * @todo need to do more extensive checks if we can always find an initial point if we only check the right half of the box
	 */
	public void setStartingPoint() {
		startAtX 						= (int) (0.5 * dimX);
		startAtY 						= 0;
		// we're starting on background, skip that..
		while (ff[startAtX][startAtY] == bg) {
			startAtY++;
		}
		// we're continuing on an edge, skip that..
		while (ff[startAtX][startAtY] == bound) {
			startAtY++;
		}
		for (int row = startAtY; row < dimY; row++, startAtY++) {
			int rowSum 					= 0;
			for (int col = startAtX; col < dimX; col++) {
				if (ff[col][row-1] != ff[col][row]) {
					rowSum 					= rowSum + ff[col][row];
				}
			}
			if ((rowSum % 2) == 1) {
				// we've found a point that's inside
				break;
			}
		}
		return;
	}
	
	/*
	 * Get initial point for flood fill algorithm
	 * 
	 */
	public int[] getStartingPoint() {
		return new int[] {startAtX, startAtY};
	}

	/*
	 * Get flood filled image
	 * 
	 */
	public int[][] getFloodFill(){
		return ff;
	}
}
