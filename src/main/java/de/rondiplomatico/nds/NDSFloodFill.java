package de.rondiplomatico.nds;

import java.awt.Point;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Utility class for flood fill algorithm applied to marked NDSTiles deduced from polygon points.
 * 
 * @author Andreas Hessenthaler
 * @since 13.02.2020
 */
public class NDSFloodFill {
    int[][] ff                  = null;
    int minX                    = Integer.MAX_VALUE;
    int maxX                    = Integer.MIN_VALUE;
    int minY                    = Integer.MAX_VALUE;
    int maxY                    = Integer.MIN_VALUE;
    int dimX                    = -1;
    int dimY                    = -1;
    int bg                      = 0;
    int bound                   = 1;
    int fill                    = bound;
    int startAtX                = 0;
    int startAtY                = 0;
    
    /**
     * Constructor
     * 
     * @param f
     *              the image input to perform flood fill on
     *              assumes:
     *              - non-empty array
     *              - closed polygon without intersections with boundary color 1 and background color 0
     * @param a
     *              the minimum x-index
     * @param b
     *              the maximum x-index
     * @param c
     *              the minimum y-index
     * @param d
     *              the maximum y-index
     */
    public NDSFloodFill(int[][] f, int a, int b, int c, int d) {
        ff                      = f;
        minX                    = a;
        maxX                    = b;
        minY                    = c;
        maxY                    = d;
        dimX                    = maxX - minX + 1;
        dimY                    = maxY - minY + 1;
        setStartingPoint();
    }
    
    /*
     * Iterative four-way flood fill algorithm
     * 
     */
    public void iterativeFloodFill() {
        Queue<Point> q             = new LinkedList<Point>();
        q.add(new Point(startAtX, startAtY));
        while (!q.isEmpty()) {
            Point p             = q.remove();
            if ((p.x < 0) || (p.y < 0) || (p.x > dimX-1 || (p.y > dimY-1))) {
                continue;
            }
            if (ff[p.x][p.y] == bg) {
                ff[p.x][p.y]     = fill;
                q.add(new Point(p.x+1, p.y  )); // right
                q.add(new Point(p.x,   p.y+1)); // top
                q.add(new Point(p.x-1, p.y  )); // left
                q.add(new Point(p.x,   p.y-1)); // bottom
            }
        }
    }
    
    /*
     * Safety net for filling left over lines after applying flood fill algorithm
     * 
     */
    public void fillHoles() {
        // first, find vertical lines
        boolean vline                     = false;
        boolean vlineEnded                = false;
        int currStartY                    = -1;
        int currStopY                    = -1;
        for (int col = 1; col < dimY-1; col++) {
            vline                         = false;
            vlineEnded                     = false;
            for (int row = 1; row < dimX-1; row++) {
                // minimum requirement: left and right neighbors are not background
                if ((ff[row][col-1] != bg) && (ff[row][col+1] != bg)) {
                    if ((ff[row][col] == bg)
                        && (ff[row-1][col] != bg)) {
                        // beginning of vertical line
                        currStartY         = row;
                        vline             = true;
                    } else if (vline && (ff[row][col] == bg)) {
                        // vertical line continues
                    } else if (vline && (ff[row][col] != bg)) {
                        // end of vertical line
                        currStopY         = row;
                        vlineEnded         = true;
                    }
                } else if (vline && ((ff[row][col+1] != bg) || (ff[row][col-1] != bg)) && (ff[row][col] != bg)) {
                    // end of horizontal line
                    currStopY         = row;
                    vlineEnded         = true;
                } else {
                    vline = false;
                    vlineEnded = false;
                }
                if (vline && vlineEnded) {
                    // fill line and reset booleans
                    System.out.println(">>>>>>INFO: Filling vertical line.");
                    for (int idx = currStartY; idx < currStopY; idx++) {
                        ff[idx][col]    = fill;
                    }
                    vline                 = false;
                    vlineEnded             = false;
                }
            }
        }
        // second, find horizontal lines
        boolean hline                     = false;
        boolean hlineEnded                = false;
        for (int row = 1; row < dimX-1; row++) {
            hline                         = false;
            hlineEnded                     = false;
            for (int col = 1; col < dimY-1; col++) {
                // minimum requirement: top and bottom neighbors are not background
                if ((ff[row+1][col] != bg) && (ff[row-1][col] != bg)) {
                    if ((ff[row][col] == bg)
                        && (ff[row][col-1] != bg)) {
                        // beginning of horizontal line
                        currStartY         = col;
                        hline             = true;
                    } else if (hline && (ff[row][col] == bg)) {
                        // vertical line continues
                    } else if (hline && (ff[row][col] != bg)) {
                        // end of vertical line
                        currStopY         = col;
                        hlineEnded         = true;
                    }
                } else if (hline && ((ff[row+1][col] != bg) || (ff[row-1][col] != bg)) && (ff[row][col] != bg)) {
                    // end of vertical line
                    currStopY         = col;
                    hlineEnded         = true;
                } else {
                    hline = false;
                    hlineEnded = false;
                }
                if (hline && hlineEnded) {
                    // fill line and reset booleans
                    System.out.println(">>>>>>INFO: Filling horizontal line.");
                    for (int idx = currStartY; idx < currStopY; idx++) {
                        ff[row][idx]    = fill;
                    }
                    hline                 = false;
                    hlineEnded             = false;
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
    private void setStartingPoint() {
        startAtX                         = (int) (0.5 * dimX);
        startAtY                         = 0;
        // we're starting on background, skip that..
        while (ff[startAtX][startAtY] == bg) {
            startAtY++;
        }
        // we're continuing on an edge, skip that..
        while (ff[startAtX][startAtY] == bound) {
            startAtY++;
        }
        for (int row = startAtY; row < dimY; row++, startAtY++) {
            int rowSum                     = 0;
            for (int col = startAtX; col < dimX; col++) {
                if (ff[col][row-1] != ff[col][row]) {
                    rowSum                     = rowSum + ff[col][row];
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
