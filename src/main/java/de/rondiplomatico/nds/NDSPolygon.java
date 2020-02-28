package de.rondiplomatico.nds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class NDSPolygon {
    
    /**
     * Applies a flood-fill algorithm to a polygon
     * 
     * @param hmi
     *              the hash map input hmi
     * @param tileNumbers
     *              the tileNumbers
     * @return
     */
    public static Map<Integer, List<Integer>> mapFillPolygon(Map<Integer, List<Integer>> hmi, int[] tileNumbers){
        Map<Integer, List<Integer>> hmo         = new HashMap<Integer, List<Integer>>();
        // first get a minimum 'bounding tile box'
        int minX                                = Integer.MAX_VALUE;
        int maxX                                = Integer.MIN_VALUE;
        int minY                                = Integer.MAX_VALUE;
        int maxY                                = Integer.MIN_VALUE;
        // loop over (key, values) pairs to get boundary
        for (Map.Entry<Integer, List<Integer>> entry : hmi.entrySet()) {
            int key                             = entry.getKey();
            List<Integer> currList              = entry.getValue();
            // we have a sorted list, so we can just get the first and last element for the comparison
            minX                                = Math.min(currList.get(0),                 minX);
            maxX                                = Math.max(currList.get(currList.size()-1), maxX);
            // key is the y-index
            minY                                = Math.min(key, minY);
            maxY                                = Math.max(key, maxY);
        }
        // get dimensions
        int dimX                                = maxX - minX + 1;
        int dimY                                = maxY - minY + 1;
        // allocate int[][] for flood fill algorithm; default value 0
        int[][] tmp                             = new int[dimX][dimY];
        // loop over (key, values) pairs to label boundary
        for (Map.Entry<Integer, List<Integer>> entry : hmi.entrySet()) {
            int key                             = entry.getKey() - minY;
            List<Integer> currList              = entry.getValue();
            // loop over values to label boundary
            for (int idx0 = 0; idx0 < currList.size(); idx0++) {
                int val                         = currList.get(idx0) - minX;
                tmp[val][key]                   = 1;
            }
        }
        // perform flood fill from midpoint
        NDSFloodFill ff                         = new NDSFloodFill(tmp, minX, maxX, minY, maxY);
        ff.iterativeFloodFill();
        tmp                                     = ff.getFloodFill();
        ff.fillHoles();
        // loop over y
        for (int key = 0; key < dimY; key++) {
            List<Integer> currList              = new ArrayList<Integer>();
            // loop over x
            for (int x = 0; x < dimX; x++) {
                // get all elements that are on boundary or inside
                if (tmp[x][key] > 0) {
                    currList.add(x+minX);
                }
            }
            hmo.put(key+minY, currList);
        }
        return hmo;
    }
    
    /**
     * Refine polygon data by subsampling.
     * 
     * @param level
     *              the level
     * @param polygonCoordinates
     *              the polygon coordinates
     * @param numSamples
     *              number of samples between each polygon edge (-1 requests adaptive sampling)
     * @return polygonCoordinatesRef
     *              the refined polygon coordinates
     */
    public static double[][] refinePolygon(int level, double[][] polygonCoordinates, int numSamples) {
        if (numSamples == 0) {
            System.out.println(">>>INFO: Polygon refinement off. Consider switching to adaptive refinement.");
            return polygonCoordinates;
        }
        if (numSamples == -1) {
            double maxDist                          = 0.0;
            double tileSizeX                        = 360.0 / (double) (Math.pow(2, level+1));
            for (int idx = 0; idx < polygonCoordinates.length-1; idx++) {
                double p0x                          = polygonCoordinates[idx  ][0];
                double p0y                          = polygonCoordinates[idx  ][1];
                double p1x                          = polygonCoordinates[idx+1][0];
                double p1y                          = polygonCoordinates[idx+1][1];
                double dist                         = Math.sqrt(Math.pow(p1x-p0x, 2.0) + Math.pow(p1y-p0y, 2.0));
                maxDist                             = Math.max(dist, maxDist);
            }
            double maxTargetDist                    = tileSizeX * 0.4;
            if (maxDist <= maxTargetDist) {
                numSamples                          = 1;
            } else {
                numSamples                          = (int) Math.ceil(maxDist / maxTargetDist);
            }
            System.out.println(">>>INFO: Setting adaptive refinement to "+numSamples);
        }
        int numCoord                                = polygonCoordinates.length;
        int numCoordRef                             = (numCoord - 1) * (numSamples + 1) + 1;
        double[][] polygonCoordinatesRef            = new double[numCoordRef][2];
        // sample between all points except for the last and first because those coincide
        for (int idx = 0; idx < numCoord-1; idx++) {
            double x0                               = polygonCoordinates[idx  ][0];
            double x1                               = polygonCoordinates[idx+1][0];
            double y0                               = polygonCoordinates[idx  ][1];
            double y1                               = polygonCoordinates[idx+1][1];
            for (int jdx = 0; jdx < numSamples+1; jdx++) {
                polygonCoordinatesRef[idx*(numSamples+1)+jdx][0]    = x0 + (x1 - x0) * (double)jdx / (double)(numSamples + 1);
                polygonCoordinatesRef[idx*(numSamples+1)+jdx][1]    = y0 + (y1 - y0) * (double)jdx / (double)(numSamples + 1);
            }
        }
        // final point to close polygon
        polygonCoordinatesRef[numCoordRef-1][0]     = polygonCoordinates[numCoord-1][0];
        polygonCoordinatesRef[numCoordRef-1][1]     = polygonCoordinates[numCoord-1][1];
        // return refined polygon
        return polygonCoordinatesRef;
    }
    
    public static int[] getTileNumbersOnLevel(int level, double[][] polygonCoordinates){
        int numCoord                                = polygonCoordinates.length;
        int[] tileNumbers                           = new int[numCoord];
        // loop over all coordinates to get map tile IDs
        for (int idx = 0; idx < numCoord; idx++) {
            NDSCoordinate currCoord                 = new NDSCoordinate(polygonCoordinates[idx][0], polygonCoordinates[idx][1]);
            NDSTile currTile                        = new NDSTile(level, currCoord);
            tileNumbers[idx]                        = currTile.getTileNumber();
        }
        return tileNumbers;
    }
    
    public static int[] getUniqueTileNumbersOnLevel(int level, double[][] polygonCoordinates){
        int[] tileNumbers                           = getTileNumbersOnLevel(level, polygonCoordinates);
        return IntStream.of(tileNumbers).distinct().toArray();
    }
}
