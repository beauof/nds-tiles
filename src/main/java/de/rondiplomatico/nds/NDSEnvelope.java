package de.rondiplomatico.nds;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.OctagonalEnvelope;

/**
 * Class to get a vividsolutions envelope of coordinates.
 * 
 * No warranties for correctness, use at own risk.
 * 
 * @author Andreas Hessenthaler
 * @since 13.02.2020
 */
public class NDSEnvelope {
    
    private static OctagonalEnvelope vividEnvelope  = new OctagonalEnvelope();
    
    public NDSEnvelope(double[][] polygonCoordinates) {
        for(int i = 0; i < polygonCoordinates.length; i++) {
            Coordinate vividCoord           = new Coordinate(polygonCoordinates[i][0], polygonCoordinates[i][1]);
            vividEnvelope.expandToInclude(vividCoord);
        }
    }
    
    public NDSEnvelope(OctagonalEnvelope e) {
        vividEnvelope                       = e;
    }
    
    public OctagonalEnvelope getEnvelope() {
        return vividEnvelope;
    }
    
    public NDSCoordinate getSouthWest() {
        return new NDSCoordinate(vividEnvelope.getMinX(), vividEnvelope.getMinY());
    }
    
    public NDSCoordinate getSouthEast() {
        return new NDSCoordinate(vividEnvelope.getMaxX(), vividEnvelope.getMinY());
    }
    
    public NDSCoordinate getNorthEast() {
        return new NDSCoordinate(vividEnvelope.getMinX(), vividEnvelope.getMaxY());
    }
    
    public NDSCoordinate getNorthWest() {
        return new NDSCoordinate(vividEnvelope.getMaxX(), vividEnvelope.getMaxY());
    }
    
    public int[] getMasterTileInfo(int maxLevels) {
        NDSCoordinate point0                = getSouthWest();
        NDSCoordinate point1                = getSouthEast();
        NDSCoordinate point2                = getNorthEast();
        NDSCoordinate point3                = getNorthWest();
        return getMasterTileInfo(point0, point1, point2, point3, maxLevels);
    }
    
    public int[] getMasterTileInfo(NDSCoordinate point0, NDSCoordinate point1, NDSCoordinate point2, NDSCoordinate point3, int maxLevels) {
        int masterTileLevel                 = -1;
        int masterTileID                    = -1;
        for (int li = 0; li < maxLevels; li++) {
            NDSTile currTile0               = new NDSTile(li, point0);
            NDSTile currTile1               = new NDSTile(li, point1);
            NDSTile currTile2               = new NDSTile(li, point2);
            NDSTile currTile3               = new NDSTile(li, point3);
            int currTileID0                 = currTile0.getTileNumber();
            int currTileID1                 = currTile1.getTileNumber();
            int currTileID2                 = currTile2.getTileNumber();
            int currTileID3                 = currTile3.getTileNumber();
            boolean singleTileID            = (currTileID0 == currTileID1) && (currTileID0 == currTileID2) && (currTileID0 == currTileID3);
            // if at least one tile ID is different, we discard the tile IDs and keep our previously detected tile ID (i.e. on the previous level)
            if(!singleTileID) {
                break;
            }
            // store tile info
            masterTileLevel                 = li;
            masterTileID                    = currTileID0;
        }
        // check if valid result
        if (masterTileID == -1) {
            System.out.println(">>>ERROR: Invalid master tile ID.");
            System.exit(1);
        }
        int[] masterTileInfo                = {masterTileLevel, masterTileID};
        return masterTileInfo;
    }
}