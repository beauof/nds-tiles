package de.rondiplomatico.nds;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.OctagonalEnvelope;

/**
 * NDSEnvelope allows to compute an envelope for a set of NDSCoordinates.
 * 
 * No warranties for correctness, use at own risk.
 * 
 * @author Andreas Hessenthaler
 * @since 13.02.2020
 */
public class NDSEnvelope {
    
    private static OctagonalEnvelope vividEnvelope  = new OctagonalEnvelope();
    
    /**
     * NDSEnvelope of a given set of polygon points
     * 
     * @param polygonCoordinates
     *              the polygonCoordinates
     */
    public NDSEnvelope(double[][] polygonCoordinates) {
        for(int i = 0; i < polygonCoordinates.length; i++) {
            Coordinate vividCoord           = new Coordinate(polygonCoordinates[i][0], polygonCoordinates[i][1]);
            vividEnvelope.expandToInclude(vividCoord);
        }
    }
    
    /**
     * NDSEnvelope constructor for OctagonalEnvelope
     * 
     * @param e
     *              the OctagonalEnvelope e
     */
    public NDSEnvelope(OctagonalEnvelope e) {
        vividEnvelope                       = e;
    }
    
    /**
     * Get the NDSEnvelope
     * 
     * @return
     */
    public OctagonalEnvelope getEnvelope() {
        return vividEnvelope;
    }
    
    /**
     * Get the SouthWest corner of the envelope
     * 
     * @return
     */
    public NDSCoordinate getSouthWest() {
        return new NDSCoordinate(vividEnvelope.getMinX(), vividEnvelope.getMinY());
    }

    /**
     * Get the SouthEast corner of the envelope
     * 
     * @return
     */
    public NDSCoordinate getSouthEast() {
        return new NDSCoordinate(vividEnvelope.getMaxX(), vividEnvelope.getMinY());
    }

    /**
     * Get the NorthEast corner of the envelope
     * 
     * @return
     */
    public NDSCoordinate getNorthEast() {
        return new NDSCoordinate(vividEnvelope.getMinX(), vividEnvelope.getMaxY());
    }

    /**
     * Get the NorthWest corner of the envelope
     * 
     * @return
     */
    public NDSCoordinate getNorthWest() {
        return new NDSCoordinate(vividEnvelope.getMaxX(), vividEnvelope.getMaxY());
    }
    
    /**
     * Get the master tile for the SouthWest, SouthEast, NorthEast, NorthWest corners for a given number of levels
     * 
     * @param maxLevels
     *              the maxLevels
     * @return masterTileInfo
     *              the masterTileInfo consisting of the {masterTileLevel, masterTileNumber}
     */
    public int[] getMasterTileInfo(int maxLevels) {
        NDSCoordinate point0                = getSouthWest();
        NDSCoordinate point1                = getSouthEast();
        NDSCoordinate point2                = getNorthEast();
        NDSCoordinate point3                = getNorthWest();
        return getMasterTileInfo(point0, point1, point2, point3, maxLevels);
    }

    /**
     * Get the master tile for a set of four NDSCoordinates for a given number of levels
     * 
     * @param maxLevels
     *              the maxLevels
     * @return masterTileInfo
     *              the masterTileInfo consisting of the {masterTileLevel, masterTileNumber}
     */
    public int[] getMasterTileInfo(NDSCoordinate point0, NDSCoordinate point1, NDSCoordinate point2, NDSCoordinate point3, int maxLevels) {
        int masterTileLevel                 = -1;
        int masterTileNumber                = -1;
        for (int li = 0; li < maxLevels; li++) {
            NDSTile currTile0               = new NDSTile(li, point0);
            NDSTile currTile1               = new NDSTile(li, point1);
            NDSTile currTile2               = new NDSTile(li, point2);
            NDSTile currTile3               = new NDSTile(li, point3);
            int currTileNumber0             = currTile0.getTileNumber();
            int currTileNumber1             = currTile1.getTileNumber();
            int currTileNumber2             = currTile2.getTileNumber();
            int currTileNumber3             = currTile3.getTileNumber();
            boolean singleTileNumber        = (currTileNumber0 == currTileNumber1) && (currTileNumber0 == currTileNumber2) && (currTileNumber0 == currTileNumber3);
            // if at least one tile ID is different, we discard the tile IDs and keep our previously detected tile ID (i.e. on the previous level)
            if(!singleTileNumber) {
                break;
            }
            // store tile info
            masterTileLevel                 = li;
            masterTileNumber                = currTileNumber0;
        }
        // check if valid result
        if (masterTileNumber == -1) {
            System.out.println(">>>ERROR: Invalid master tile ID.");
            System.exit(1);
        }
        return new int[] {masterTileLevel, masterTileNumber};
    }
}