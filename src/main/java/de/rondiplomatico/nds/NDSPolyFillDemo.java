package de.rondiplomatico.nds;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.rondiplomatico.nds.NDSUtils;
import de.rondiplomatico.nds.NDSHashmap;

public class NDSPolyFillDemo {
    
    /**
     * Main method to test selecting all tiles covered by a sample polygon.
     * 
     * @param args
     */
    @Test
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
        int tstLevel                                = 11;
        // refine polygon coordinates to avoid edges that are crossing a tile
        // set refinement factor to -1 to adaptively refine
        int numSamples                              = -1;
        double[][] polygonCoordinatesRef            = NDSPolygon.refinePolygon(tstLevel, polygonCoordinates, numSamples);
        // let's grab all tiles with polygon points
        int[] uniqueTileIDs                         = NDSPolygon.getUniqueTileNumbersOnLevel(tstLevel, polygonCoordinatesRef);
        // dump to image file for debugging
        NDSUtils.printMap(masterTile, tstLevel, uniqueTileIDs, "png", "map");
        // get x/y tile indices for tiles with polygon points
        // key is tileY, val is tileX (note: val may contain multiple tileX indices)
        NDSHashmap hm                               = new NDSHashmap();
        Map<Integer, List<Integer>> tileHM          = hm.tileNumbersToHM(tstLevel, uniqueTileIDs);
        // dump to image file for debugging
        Map<Integer, List<Integer>> filledTileHM    = NDSPolygon.mapFillPolygon(tileHM, uniqueTileIDs);
        int[] filledTileIDs                         = hm.hmToTileNumbers(tstLevel, filledTileHM);
        NDSUtils.printMap(masterTile, tstLevel, filledTileIDs, "png", "mapFilled");
        
        long t1                                     = System.currentTimeMillis();
        System.out.println("\n>>>INFO: Program finished in "+(t1-t0)+" ms.");
    }
}
