package de.rondiplomatico.nds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class polygon {
    
    /**
     * Main method to test selecting all tiles covered by a sample polygon.
     * 
     * @param args
     */
    public static void main(String[] args) {

        long t0                                     = System.currentTimeMillis();

        // get some random polygon data for testing purposes
//        double[][] polygonCoordinates = new double[][] {
//            {10.5,  45.9},
//            {13.0,  50.3},
//            {15.0,  47.0},
//            {13.4,  70.0},
//            {10.5,  45.9}
//        };
        
        // get some random polygon data for testing purposes
//        double[][] polygonCoordinates = new double[][] {
//            {10.5,  45.9},
//            {13.0,  63.3},
//            {15.0,  47.0},
//            {13.4,  70.0},
//            {10.5,  45.9}
//        };
        
        // get some random polygon data for testing purposes
//        double[][] polygonCoordinates = new double[][] {
//            {10.5,  45.9},
//            {13.0,  50.3},
//            {15.0,  47.0},
//            {17.0,  50.3},
//            {20.0,  47.0},
//            {13.4,  60.0},
//            {13.4,  70.0},
//            {10.5,  45.9}
//        };
        
        // get some random polygon data approximating Germany
        // https://www.mapsofworld.com/lat_long/germany-lat-long.html
        double[][] polygonCoordinates = new double[][] {
            {10.5,  45.9},
            {13.0,  45.9},
            {14.0,  49.0},
            {12.0,  50.0},
            {15.0,  51.0},
            {15.0,  54.0},
            {13.5,  54.5},
            {11.0,  54.0},
            {10.0,  55.0},
            { 8.5,  55.0},
            { 9.0,  54.0},
            { 7.0,  53.5},
            { 6.0,  52.0},
            { 6.1,  50.0},
            { 8.0,  49.0},
            { 7.5,  47.5},
            {10.5,  45.9}
        };
        
        // number of levels in map hierarchy
        int maxLevels                               = 15;
        // get a bounding octagonal envelope (defaults to quadrilateral in 2D case)
        //         for sample data corresponding to a polygon (e.g. borders of a country)
        NDSEnvelope envelope                        = new NDSEnvelope(polygonCoordinates);
        // get corresponding bounding tile ID, i.e. find level where all polygon points are on the same tile
        int[] masterTileInfo                        = envelope.getMasterTileInfo(maxLevels);
        int masterTileLevel                         = masterTileInfo[0];
        int masterTileID                            = masterTileInfo[1];
        // store the master tile
        NDSTile masterTile                          = new NDSTile(masterTileLevel, masterTileID);
        // get all tiles covered by the polygon on the tstLevel
        int tstLevel                                = 8; // 13
        // refine polygon coordinates to avoid edges that are crossing a tile
        // set refinement factor to -1 to adaptively refine
        int numSamples                              = -1;
        double[][] polygonCoordinatesRef            = polygon.refinePolygon(tstLevel, polygonCoordinates, numSamples);
        // let's grab all tiles with polygon points
        int[] uniqueTileIDs                         = polygon.getUniqueTileNumbersOnLevel(tstLevel, polygonCoordinatesRef);
        // dump to image file for debugging
        masterTile.printMap(tstLevel, uniqueTileIDs, "png", "map");
        // get x/y tile indices for tiles with polygon points
        // key is tileY, val is tileX (note: val may contain multiple tileX indices)
        Map<Integer, List<Integer>> tileHM          = masterTile.tileNumbersToHM(tstLevel, uniqueTileIDs);
        // dump to image file for debugging
        Map<Integer, List<Integer>> filledTileHM    = polygon.mapFillPolygon(tileHM, uniqueTileIDs, "flood-fill-safe");
        int[] filledTileIDs                         = masterTile.hmToTileNumbers(tstLevel, filledTileHM);
        masterTile.printMap(tstLevel, filledTileIDs, "png", "mapFilled");
        
        long t1                                     = System.currentTimeMillis();
        System.out.println("\n>>>INFO: Program finished in "+(t1-t0)+" ms.");
    }
    
    /**
     * Applies a flood-fill algorithm to a polygon
     * 
     * @param hmi
     *              the hash map input hmi
     * @param tileNumbers
     *              the tileNumbers
     * @param method
     *              the flood-fill algorithm (flood-fill, row-fill)
     * @return
     */
    public static Map<Integer, List<Integer>> mapFillPolygon(Map<Integer, List<Integer>> hmi, int[] tileNumbers, String method){
        Map<Integer, List<Integer>> hmo             = new HashMap<Integer, List<Integer>>();
        if (method.contains("flood-fill")) {
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
            
            floodFill ff                            = new floodFill(tmp, minX, maxX, minY, maxY);
            ff.iterativeFloodFill();
            tmp                                     = ff.getFloodFill();
            if (method.equalsIgnoreCase("flood-fill-safe")) {
                ff.fillHoles();
            }
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
        } else if (method.contains("row-fill")) {
            System.out.println(">>>WARNING: Line-filling may not respect boundaries of concave shape. Use flood fill algorithm instead.");
            for (Map.Entry<Integer, List<Integer>> entry : hmi.entrySet()) {
                int key                             = entry.getKey();
                List<Integer> currList              = entry.getValue();
                for (int idx0 = 0; idx0 < currList.size()-1; idx0++) {
                    int currVal                     = currList.get(idx0);
                    int nextVal                     = currList.get(idx0+1);
                    if (currVal+1 < nextVal) {
                        currList.add(idx0+1, currVal+1);
                    }
                }
                hmo.put(key, currList);
            }
        } else {
            System.out.println(">>>ERROR: Unknown method "+method);
            System.exit(1);
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
