package de.rondiplomatico.nds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.vividsolutions.jts.geom.OctagonalEnvelope;

import de.rondiplomatico.nds.NDSBBox;
import de.rondiplomatico.nds.NDSCoordinate;
import de.rondiplomatico.nds.NDSTile;
import de.rondiplomatico.nds.WGS84BBox;
import de.rondiplomatico.nds.WGS84Coordinate;

/**
 * Tests the NDSTile class.
 * 
 *
 * @author Daniel Wirtz
 * @since 20.02.2020
 */
public class NDSTileTest {

    private final double eps = 1E-7;

    @Test
    public void testFixedData() {
        // Barcelona area
        NDSTile t = new NDSTile(539636700);
        assertEquals(13, t.getLevel());
        assertEquals(2765788, t.getTileNumber());
        assertEquals(new NDSCoordinate(24772607, 493486079), t.getCenter());
        assertEquals(new NDSBBox(493617151, 24903679, 493355008, 24641536), t.getBBox());

        System.out.println(t);
        System.out.println(t.toGeoJSON());

        t = new NDSTile(10, new WGS84Coordinate(30, -34));
        assertEquals(675564, t.getTileNumber());
    }

    @Test
    public void testFromCoordinateWorks() {
        NDSTile t = new NDSTile(539636700);
        NDSCoordinate c = new NDSCoordinate(24772607, 493486079);
        assertEquals(t, new NDSTile(13, c));

        NDSBBox b = t.getBBox();

        assertEquals(t, new NDSTile(13, b.northEast()));
        assertEquals(t, new NDSTile(13, b.northWest()));
        assertEquals(t, new NDSTile(13, b.southEast()));
        assertEquals(t, new NDSTile(13, b.southWest()));

        assertEquals(new NDSTile(134390589), new NDSTile(11, c));
        assertEquals(new NDSTile(269126903), new NDSTile(12, c));
        assertEquals(new NDSTile(539636700), new NDSTile(13, c));
        assertEquals(new NDSTile(1084804976), new NDSTile(14, c));
        assertEquals(new NDSTile(-2103231037), new NDSTile(15, c));

        System.out.println(b);
        System.out.println(b.toGeoJSON());
    }

    @Test
    public void packedIDRoundTripWorks() {
        assertEquals(Integer.MAX_VALUE, new NDSTile(Integer.MAX_VALUE).packedId());
        assertEquals(Integer.MIN_VALUE, new NDSTile(Integer.MIN_VALUE).packedId());
        assertEquals(1 << 16, new NDSTile(1 << 16).packedId());
        for (int i = 0; i < 1000; i++) {
            int id = (int) Math.round(Math.random() * Integer.MAX_VALUE) + (1 << 16);
            assertEquals(id, new NDSTile(id).packedId());

            id = (int) Math.round(Math.random() * Integer.MIN_VALUE);
            assertEquals(id, new NDSTile(id).packedId());
        }
    }

    @Test
    public void testContainsWorks() {
        NDSTile t = new NDSTile(539636700);
        NDSCoordinate c = new NDSCoordinate(24772607, 493486079);
        assertTrue(t.contains(c));

        NDSBBox b = t.getBBox();

        assertTrue(t.contains(b.northEast()));
        assertTrue(t.contains(b.northWest()));
        assertTrue(t.contains(b.southEast()));
        assertTrue(t.contains(b.southWest()));
        assertTrue(t.contains(b.center()));
        assertFalse(t.contains(b.northEast().add(30, 30)));
        assertFalse(t.contains(b.southWest().add(-30, -30)));
    }

    @Test
    public void testBoundingBoxWorks() {

        // Tile 0
        NDSTile t = new NDSTile(0, 0);
        NDSBBox bb = t.getBBox();
        assertEquals(NDSCoordinate.MAX_LATITUDE, bb.getNorth());
        assertEquals(NDSCoordinate.MAX_LONGITUDE, bb.getEast());
        assertEquals(NDSCoordinate.MIN_LATITUDE, bb.getSouth());
        assertEquals(0, bb.getWest());
        WGS84BBox wgsbox = bb.toWGS84();
        assertEquals(90.0, wgsbox.getNorth(), eps);
        assertEquals(180.0, wgsbox.getEast(), eps);
        assertEquals(-90.0, wgsbox.getSouth(), eps);
        assertEquals(0, wgsbox.getWest(), eps);

        // Tile 1
        t = new NDSTile(0, 1);
        bb = t.getBBox();
        assertEquals(NDSCoordinate.MAX_LATITUDE, bb.getNorth());
        assertEquals(0, bb.getEast());
        assertEquals(NDSCoordinate.MIN_LATITUDE, bb.getSouth());
        assertEquals(NDSCoordinate.MIN_LONGITUDE, bb.getWest());
        wgsbox = bb.toWGS84();
        assertEquals(90.0, wgsbox.getNorth(), eps);
        assertEquals(0.0, wgsbox.getEast(), eps);
        assertEquals(-90.0, wgsbox.getSouth(), eps);
        assertEquals(-180.0, wgsbox.getWest(), eps);

        // Tile 0-00
        t = new NDSTile(1, 0);
        bb = t.getBBox();
        assertEquals(NDSCoordinate.MAX_LATITUDE, bb.getNorth());
        assertEquals(NDSCoordinate.MAX_LONGITUDE / 2, bb.getEast());
        assertEquals(0, bb.getSouth());
        assertEquals(0, bb.getWest());
        wgsbox = bb.toWGS84();
        assertEquals(90.0, wgsbox.getNorth(), eps);
        assertEquals(90.0, wgsbox.getEast(), eps);
        assertEquals(0.0, wgsbox.getSouth(), eps);
        assertEquals(0.0, wgsbox.getWest(), eps);

        // Tile 0-01
        t = new NDSTile(1, 1);
        bb = t.getBBox();
        assertEquals(NDSCoordinate.MAX_LATITUDE, bb.getNorth());
        assertEquals(NDSCoordinate.MAX_LONGITUDE, bb.getEast());
        assertEquals(0, bb.getSouth());
        assertEquals(NDSCoordinate.MAX_LONGITUDE / 2 + 1, bb.getWest());
        wgsbox = bb.toWGS84();
        assertEquals(90.0, wgsbox.getNorth(), eps);
        assertEquals(180.0, wgsbox.getEast(), eps);
        assertEquals(0.0, wgsbox.getSouth(), eps);
        assertEquals(90.0, wgsbox.getWest(), eps);

        // Tile 0-10
        t = new NDSTile(1, 2);
        bb = t.getBBox();
        assertEquals(0, bb.getNorth());
        assertEquals(NDSCoordinate.MAX_LONGITUDE / 2, bb.getEast());
        assertEquals(NDSCoordinate.MIN_LATITUDE, bb.getSouth());
        assertEquals(0, bb.getWest());
        wgsbox = bb.toWGS84();
        assertEquals(0.0, wgsbox.getNorth(), eps);
        assertEquals(90.0, wgsbox.getEast(), eps);
        assertEquals(-90.0, wgsbox.getSouth(), eps);
        assertEquals(0.0, wgsbox.getWest(), eps);

        // Tile 0-11
        t = new NDSTile(1, 3);
        bb = t.getBBox();
        assertEquals(0, bb.getNorth());
        assertEquals(NDSCoordinate.MAX_LONGITUDE, bb.getEast());
        assertEquals(NDSCoordinate.MIN_LATITUDE, bb.getSouth());
        assertEquals(NDSCoordinate.MAX_LONGITUDE / 2 + 1, bb.getWest());
        wgsbox = bb.toWGS84();
        assertEquals(0.0, wgsbox.getNorth(), eps);
        assertEquals(180.0, wgsbox.getEast(), eps);
        assertEquals(-90.0, wgsbox.getSouth(), eps);
        assertEquals(90.0, wgsbox.getWest(), eps);

        // Tile 1-00
        t = new NDSTile(1, 4);
        bb = t.getBBox();
        assertEquals(NDSCoordinate.MAX_LATITUDE, bb.getNorth());
        assertEquals(NDSCoordinate.MIN_LONGITUDE / 2, bb.getEast());
        assertEquals(0, bb.getSouth());
        assertEquals(NDSCoordinate.MIN_LONGITUDE, bb.getWest());
        wgsbox = bb.toWGS84();
        assertEquals(90.0, wgsbox.getNorth(), eps);
        assertEquals(-90.0, wgsbox.getEast(), eps);
        assertEquals(0.0, wgsbox.getSouth(), eps);
        assertEquals(-180.0, wgsbox.getWest(), eps);

        // Tile 1-01
        t = new NDSTile(1, 5);
        bb = t.getBBox();
        assertEquals(NDSCoordinate.MAX_LATITUDE, bb.getNorth());
        assertEquals(0, bb.getEast());
        assertEquals(0, bb.getSouth());
        assertEquals(NDSCoordinate.MIN_LONGITUDE / 2, bb.getWest());
        wgsbox = bb.toWGS84();
        assertEquals(90.0, wgsbox.getNorth(), eps);
        assertEquals(0.0, wgsbox.getEast(), eps);
        assertEquals(0.0, wgsbox.getSouth(), eps);
        assertEquals(-90.0, wgsbox.getWest(), eps);

        // Tile 1-10
        t = new NDSTile(1, 6);
        bb = t.getBBox();
        assertEquals(0, bb.getNorth());
        assertEquals(NDSCoordinate.MIN_LONGITUDE / 2, bb.getEast());
        assertEquals(NDSCoordinate.MIN_LATITUDE, bb.getSouth());
        assertEquals(NDSCoordinate.MIN_LONGITUDE, bb.getWest());
        wgsbox = bb.toWGS84();
        assertEquals(0.0, wgsbox.getNorth(), eps);
        assertEquals(-90.0, wgsbox.getEast(), eps);
        assertEquals(-90.0, wgsbox.getSouth(), eps);
        assertEquals(-180.0, wgsbox.getWest(), eps);

        // Tile 1-11
        t = new NDSTile(1, 7);
        bb = t.getBBox();
        assertEquals(0, bb.getNorth());
        assertEquals(0, bb.getEast());
        assertEquals(NDSCoordinate.MIN_LATITUDE, bb.getSouth());
        assertEquals(NDSCoordinate.MIN_LONGITUDE / 2, bb.getWest());
        wgsbox = bb.toWGS84();
        assertEquals(0.0, wgsbox.getNorth(), eps);
        assertEquals(0.0, wgsbox.getEast(), eps);
        assertEquals(-90.0, wgsbox.getSouth(), eps);
        assertEquals(-90.0, wgsbox.getWest(), eps);
    }

    @Test
    public void testCenterWorks() {
        // Tile 0
        NDSTile t = new NDSTile(0, 0);
        NDSCoordinate c = t.getCenter();
        assertEquals(0, c.getLatitude());
        assertEquals(NDSCoordinate.MAX_LONGITUDE / 2, c.getLongitude());
        WGS84Coordinate wgs = c.toWGS84();
        assertEquals(0.0, wgs.getLatitude(), eps);
        assertEquals(90.0, wgs.getLongitude(), eps);

        // Tile 1
        t = new NDSTile(0, 1);
        c = t.getCenter();
        assertEquals(0, c.getLatitude());
        assertEquals(NDSCoordinate.MIN_LONGITUDE / 2, c.getLongitude());
        wgs = c.toWGS84();
        assertEquals(0.0, wgs.getLatitude(), eps);
        assertEquals(-90.0, wgs.getLongitude(), eps);

        // Tile 1-11
        t = new NDSTile(1, 7);
        c = t.getCenter();
        assertEquals(NDSCoordinate.MIN_LATITUDE / 2, c.getLatitude());
        assertEquals(NDSCoordinate.MIN_LONGITUDE / 4, c.getLongitude());
        wgs = c.toWGS84();
        assertEquals(-45.0, wgs.getLatitude(), eps);
        assertEquals(-45.0, wgs.getLongitude(), eps);

        // Tile 0-01-01
        t = new NDSTile(2, 5);
        c = t.getCenter();
        assertEquals(Math.floor(NDSCoordinate.MAX_LATITUDE / 4.0), c.getLatitude(), eps);
        assertEquals(Math.floor(NDSCoordinate.MAX_LONGITUDE * 7.0 / 8.0), c.getLongitude(), eps);
        wgs = c.toWGS84();
        assertEquals(90.0 / 4.0, wgs.getLatitude(), eps); // 22.5
        assertEquals(180.0 * 7.0 / 8.0, wgs.getLongitude(), eps); // 112.5

        // Tile 1-11-10
        t = new NDSTile(2, 30);
        c = t.getCenter();
        assertEquals(Math.floor(NDSCoordinate.MIN_LATITUDE / 4.0), c.getLatitude(), eps);
        assertEquals(Math.floor(NDSCoordinate.MIN_LONGITUDE * 3.0 / 8.0), c.getLongitude(), eps);
        wgs = c.toWGS84();
        assertEquals(-90.0 / 4.0, wgs.getLatitude(), eps);
        assertEquals(-180 * 3.0 / 8.0, wgs.getLongitude(), eps);

        // Automated loop for the last and first tile of every level - there's a pattern :-)
        for (int lvl = 3; lvl < 16; lvl++) {
            t = new NDSTile(lvl, 0);
            c = t.getCenter();
            double latdiv = (double) (1L << lvl);
            double londiv = (double) (1L << lvl + 1);
            assertEquals(Math.floor(NDSCoordinate.MAX_LATITUDE / latdiv), c.getLatitude(), eps);
            assertEquals(Math.floor(NDSCoordinate.MAX_LONGITUDE / londiv), c.getLongitude(), eps);
            wgs = c.toWGS84();
            assertEquals(90.0 / latdiv, wgs.getLatitude(), eps);
            assertEquals(180.0 / londiv, wgs.getLongitude(), eps);

            t = new NDSTile(lvl, (1 << 2 * lvl + 1) - 1);
            c = t.getCenter();
            assertEquals("Latitude of Tile " + t.getTileNumber() + " on level " + lvl,
                         Math.floor(NDSCoordinate.MIN_LATITUDE / latdiv), c.getLatitude(), eps);
            assertEquals("Longitude of Tile " + t.getTileNumber() + " on level " + lvl,
                         Math.floor(NDSCoordinate.MIN_LONGITUDE / londiv), c.getLongitude(), eps);
            wgs = c.toWGS84();
            assertEquals(-90.0 / latdiv, wgs.getLatitude(), eps);
            assertEquals(-180.0 / londiv, wgs.getLongitude(), eps);
        }
    }

    @Test
    public void testCornersWorks() {
        // Tile 0
        NDSTile t               = new NDSTile(0, 0);
        NDSCoordinate c         = t.getCenter();
        NDSCoordinate[] corners = t.getCorners();
        NDSCoordinate sw        = corners[0];
        NDSCoordinate se        = corners[1];
        NDSCoordinate ne        = corners[2];
        NDSCoordinate nw        = corners[3];
        System.out.println(sw.toWGS84());
        System.out.println(se.toWGS84());
        System.out.println(nw.toWGS84());
        System.out.println(ne.toWGS84());
        assertEquals(  0.0, sw.toWGS84().getLongitude(), eps);
        assertEquals(180.0, se.toWGS84().getLongitude(), eps);
        assertEquals(  0.0, nw.toWGS84().getLongitude(), eps);
        assertEquals(180.0, ne.toWGS84().getLongitude(), eps);
        assertEquals(-90.0, sw.toWGS84().getLatitude(),  eps);
        assertEquals(-90.0, se.toWGS84().getLatitude(),  eps);
        assertEquals( 90.0, nw.toWGS84().getLatitude(),  eps);
        assertEquals( 90.0, ne.toWGS84().getLatitude(),  eps);
        // Tile 1
        t               = new NDSTile(0, 1);
        c         = t.getCenter();
        corners = t.getCorners();
        sw        = corners[0];
        se        = corners[1];
        ne        = corners[2];
        nw        = corners[3];
        System.out.println(sw.toWGS84());
        System.out.println(se.toWGS84());
        System.out.println(nw.toWGS84());
        System.out.println(ne.toWGS84());
        assertEquals(-180.0, sw.toWGS84().getLongitude(), eps);
        assertEquals(   0.0, se.toWGS84().getLongitude(), eps);
        assertEquals(-180.0, nw.toWGS84().getLongitude(), eps);
        assertEquals(   0.0, ne.toWGS84().getLongitude(), eps);
        assertEquals(-90.0, sw.toWGS84().getLatitude(),  eps);
        assertEquals(-90.0, se.toWGS84().getLatitude(),  eps);
        assertEquals( 90.0, nw.toWGS84().getLatitude(),  eps);
        assertEquals( 90.0, ne.toWGS84().getLatitude(),  eps);
    }

    @Test
    public void testMidpointsWorks() {
        // Tile 0
        NDSTile t               = new NDSTile(0, 0);
        NDSCoordinate c         = t.getCenter();
        NDSCoordinate[] corners = t.getCorners();
        NDSCoordinate sw        = corners[0];
        NDSCoordinate se        = corners[1];
        NDSCoordinate ne        = corners[2];
        NDSCoordinate nw        = corners[3];
        NDSCoordinate csw       = sw.getMidpoint(c);
        NDSCoordinate cse       = se.getMidpoint(c);
        NDSCoordinate cne       = ne.getMidpoint(c);
        NDSCoordinate cnw       = nw.getMidpoint(c);
        assertEquals( 45.0, csw.toWGS84().getLongitude(), eps);
        assertEquals(135.0, cse.toWGS84().getLongitude(), eps);
        assertEquals( 45.0, cnw.toWGS84().getLongitude(), eps);
        assertEquals(135.0, cne.toWGS84().getLongitude(), eps);
        assertEquals(-45.0, csw.toWGS84().getLatitude(),  eps);
        assertEquals(-45.0, cse.toWGS84().getLatitude(),  eps);
        assertEquals( 45.0, cnw.toWGS84().getLatitude(),  eps);
        assertEquals( 45.0, cne.toWGS84().getLatitude(),  eps);
        // Tile 1
        t                       = new NDSTile(0, 1);
        c                       = t.getCenter();
        corners                 = t.getCorners();
        sw                      = corners[0];
        se                      = corners[1];
        ne                      = corners[2];
        nw                      = corners[3];
        csw                     = sw.getMidpoint(c);
        cse                     = se.getMidpoint(c);
        cne                     = ne.getMidpoint(c);
        cnw                     = nw.getMidpoint(c);
        assertEquals(-135.0, csw.toWGS84().getLongitude(), eps);
        assertEquals( -45.0, cse.toWGS84().getLongitude(), eps);
        assertEquals(-135.0, cnw.toWGS84().getLongitude(), eps);
        assertEquals( -45.0, cne.toWGS84().getLongitude(), eps);
        assertEquals( -45.0, csw.toWGS84().getLatitude(),  eps);
        assertEquals( -45.0, cse.toWGS84().getLatitude(),  eps);
        assertEquals(  45.0, cnw.toWGS84().getLatitude(),  eps);
        assertEquals(  45.0, cne.toWGS84().getLatitude(),  eps);
    }

    @Test
    public void testChildTileNumbersWorks() {
        // get tile and its center
        int masterTileLevel                     = 3;
        NDSTile t                               = new NDSTile(masterTileLevel, 8);
        NDSCoordinate c                         = t.getCenter();
        // store the master tile corners; ordering is: sw se ne nw
        NDSCoordinate[] corners                 = t.getCorners();
        NDSCoordinate sw                        = corners[0];
        NDSCoordinate se                        = corners[1];
        NDSCoordinate ne                        = corners[2];
        NDSCoordinate nw                        = corners[3];
        // first, let's create four points that we know are in the child tiles
        int childTileLevel                      = masterTileLevel + 1;
        NDSCoordinate csw                       = sw.getMidpoint(c);
        NDSCoordinate cse                       = se.getMidpoint(c);
        NDSCoordinate cne                       = ne.getMidpoint(c);
        NDSCoordinate cnw                       = nw.getMidpoint(c);
        // get child tiles based on master level for double-checking
        NDSTile tsw                             = new NDSTile(childTileLevel, csw);
        NDSTile tse                             = new NDSTile(childTileLevel, cse);
        NDSTile tne                             = new NDSTile(childTileLevel, cne);
        NDSTile tnw                             = new NDSTile(childTileLevel, cnw);
        int swNumber                            = t.getChildTileNumberSouthWest();
        int seNumber                            = t.getChildTileNumberSouthEast();
        int neNumber                            = t.getChildTileNumberNorthEast();
        int nwNumber                            = t.getChildTileNumberNorthWest();
        // now perform check
        assertEquals(tsw.getTileNumber(), swNumber);
        assertEquals(tse.getTileNumber(), seNumber);
        assertEquals(tne.getTileNumber(), neNumber);
        assertEquals(tnw.getTileNumber(), nwNumber);
    }

    @Test
    public void testQuadkeyWorks() {
        // rough polygon data approximating Germany
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
        int maxLevels                           = 15;
        // get a bounding octagonal envelope (defaults to quadrilateral in 2D case)
        //         for sample data corresponding to a polygon (e.g. borders of a country)
        NDSEnvelope envelope                = new NDSEnvelope(polygonCoordinates);
        // get corresponding bounding tile ID, i.e. find level where all polygon points are on the same tile
        int[] masterTileInfo                    = envelope.getMasterTileInfo(maxLevels);
        int masterTileLevel                     = masterTileInfo[0];
        int masterTileID                        = masterTileInfo[1];
        // store the master tile
        NDSTile masterTile                      = new NDSTile(masterTileLevel, masterTileID);
        // set some random tile number and level for testing
        int tstTileNumber                       = 7;
        int tstTileLevel                        = 3;
        int[] tstTileXY                         = masterTile.getTileXYfromTileID(tstTileLevel, tstTileNumber);
        int tstTileX                            = tstTileXY[0];
        int tstTileY                            = tstTileXY[1];
        assertEquals(11, tstTileX);
        assertEquals( 5, tstTileY);
        String tstTileQuadkey                   = masterTile.getTileQuadkeyFromTileXY(tstTileLevel, tstTileX, tstTileY);
        assertEquals("1213",              tstTileQuadkey);
        assertEquals("00313102310",       masterTile.getTileQuadkeyFromTileXY(10, 486, 332));
        assertEquals("01202102332221212", masterTile.getTileQuadkeyFromTileXY(16, 35210, 21493));
        // inverse direction
        tstTileXY                               = masterTile.getTileXYfromTileQuadkey(tstTileQuadkey);
        assertEquals(tstTileX, tstTileXY[0]);
        assertEquals(tstTileY, tstTileXY[1]);
        assertEquals(tstTileNumber, masterTile.getTileIDfromTileXY(tstTileLevel, tstTileX, tstTileY));
    }
    
    @Test
    public void testEnvelopeWorks() {
        // rough polygon data approximating Germany
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
        NDSEnvelope envelope                = new NDSEnvelope(polygonCoordinates);
        OctagonalEnvelope vividEnvelope     = envelope.getEnvelope();
        // check envelope boundaries
        assertEquals( 6.0, vividEnvelope.getMinX(), eps);
        assertEquals(15.0, vividEnvelope.getMaxX(), eps);
        assertEquals(45.9, vividEnvelope.getMinY(), eps);
        assertEquals(55.0, vividEnvelope.getMaxY(), eps);
        // check bounding box corners
        assertEquals(581131592357515410L, envelope.getSouthWest().getMortonCode());
        assertEquals(595825689965249734L, envelope.getSouthEast().getMortonCode());
        assertEquals(592806849050488888L, envelope.getNorthEast().getMortonCode());
        assertEquals(607500946658223212L, envelope.getNorthWest().getMortonCode());
        // check master tile details
        int[] info                          = envelope.getMasterTileInfo(15);
        int level                           = info[0];
        int number                          = info[1];
        assertEquals(3, level);
        assertEquals(8, number);
    }

    @Test
    public void testConstructor() {
        try {
            NDSTile t = new NDSTile(34);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            NDSTile t = new NDSTile(-1, 0);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            NDSTile t = new NDSTile(0, -1);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            NDSTile t = new NDSTile(2, Integer.MAX_VALUE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        NDSTile t = new NDSTile(2, 5);
        NDSTile t2 = new NDSTile(2, 5);
        assertTrue(t.equals(t2));
    }
}
