package de.rondiplomatico.nds;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Implementation of the NDS Tile scheme.
 * It follows the NDS Format Specification, Version 2.5.4, §7.3.1.
 * 
 * No warranties for correctness, use at own risk.
 *
 * @author Daniel Wirtz
 * @author Andreas Hessenthaler
 * @since 20.02.2020
 */
@Getter
@ToString
@EqualsAndHashCode
public class NDSTile {

    /**
     * The maximum Tile level within the NDS specification.
     */
    public static final int MAX_LEVEL = 15;

    /*
     * The tile level
     */
    private int level = -1;

    /*
     * The tile number.
     * 
     * The tile number is identical to the (2*level+1) most-significant bits of
     * the Morton code of the south-west corner of the tile.
     */
    private int tileNumber;

    /*
     * Transient center coordinate
     */
    private transient NDSCoordinate center;

    /**
     * Creates a new {@link NDSTile} instance from a packed Tile id.
     *
     * @param packedId
     * @see NDSSpecification 2.5.4: 7.3.3 Generating Packed Tile IDs
     */
    public NDSTile(int packedId) {
        level = extractLevel(packedId);
        if (level < 0) {
            throw new IllegalArgumentException("Invalid packed Tile ID " + packedId + ": No Level bit present.");
        }
        int level_bit = 1 << (16 + level);
        tileNumber = packedId ^ level_bit;
    }

    /**
     * Creates a new {@link NDSTile} instance for a given id and level.
     *
     * @param level
     *                  Must be in range 0..15
     * @param nr
     *                  An admissible tile number w.r.t to the specified level.
     * 
     * @see NDSSpecification 2.5.4: 7.3.3 Generating Packed Tile IDs
     */
    public NDSTile(int level, int nr) {
        if (level < 0) {
            throw new IllegalArgumentException("The Tile level " + level + " exceeds the range [0, 15].");
        }
        this.level = level;
        if (nr < 0) {
            throw new IllegalArgumentException("The Tile id " + level + " must be positive (Max length is 31 bits).");
        }
        if (nr > (1 << 2 * level + 1) - 1) {
            throw new IllegalArgumentException("Invalid Tile number for level " + level + ", numbers 0 .. " + (Math.pow(2, 2 * level + 1) - 1)
                            + " are allowed");
        }
        this.tileNumber = nr;
    }

    /**
     * 
     * Creates a new {@link NDSTile} instance of the specified level, containing the specified coordinate
     *
     * @param level
     * @param coord
     */
    public NDSTile(int level, NDSCoordinate coord) {
        /*
         * Getting the NDS tile for a NDS coordinate amount to shifting the morton code of the coordinate by the necessary
         * amount. Each NDSTile can be represented by the level and morton code of the lower left / south west corner.
         */
        this(level, (int) (coord.getMortonCode() >> 32 + (MAX_LEVEL - level) * 2));
    }

    /**
     * Creates a new {@link NDSTile} instance of the specified level, containing the specified coordinate
     *
     * @param level
     *                  the level
     * @param coord
     *                  the coord
     */
    public NDSTile(int level, WGS84Coordinate coord) {
        this(level, new NDSCoordinate(coord.getLongitude(), coord.getLatitude()));
    }

    /**
     * Get tile indices in x / y from id and level.
     */
    public int[] getTileXY() {
        // variables for storing x/y-tile indices
        int tileX, tileY;
        tileX = tileY                       = -1;
        // get tile size, assuming that there are 2x1 tiles on level 0
        double tileSizeX, tileSizeY;
        tileSizeX                           = 360.0 / (double) (Math.pow(2, level+1));
        tileSizeY                           = 180.0 / (double) (Math.pow(2, level  ));
        // get tile center and southwest corner
        NDSCoordinate c                     = getCenter();
        double lon                          = c.toWGS84().getLongitude() - 0.5 * tileSizeX;
        double lat                          = c.toWGS84().getLatitude()  - 0.5 * tileSizeY;
        // compute tile indices
        tileX                               = (int) Math.round((lon + 180.0) / tileSizeX);
        tileY                               = (int) Math.round((lat +  90.0) / tileSizeY);
        return new int[] {tileX, tileY};
    }

    /**
     * Get tile indices in x / y from given id and level.
     * 
     * @param level
     *                 the level
     * @param nr
     *                 the tile number
     */
    public int[] getTileXYfromTileNumber(int level, int nr) {
        NDSTile tile                        = new NDSTile(level, nr);
        return tile.getTileXY();
    }

    /**
     * Get tile number from indices in x / y and level.
     * 
     * @param level
     *                 the level
     * @param tileX
     *                 the tile x-coordinate
     * @param tileY
     *                 the tile y-coordinate
     */
    public int getTileNumberfromTileXY(int level, int tileX, int tileY) {
        // get tile size, assuming that there are 2x1 tiles on level 0
        double tileSizeX                    = 360.0 / (double) (Math.pow(2, level+1));
        double tileSizeY                    = 180.0 / (double) (Math.pow(2, level  ));
        // get center
        double clon                         = tileX * tileSizeX + 0.5 * tileSizeX - 180.0;
        double clat                         = tileY * tileSizeY + 0.5 * tileSizeY -  90.0;
        NDSCoordinate c                     = new NDSCoordinate(clon, clat);
        // get tile
        NDSTile t                           = new NDSTile(level, c);
        // return tile number
        return t.getTileNumber();
    }

    /**
     * Get tile quadkey from tile indices in x / y and level.
     * 
     * @param level
     *                 the level
     * @param tileX
     *                 the tile x-coordinate
     * @param tileY
     *                 the tile y-coordinate
     */
    public String getTileQuadkeyFromTileXY(int level, int tileX, int tileY) {
        // setup object for building quadkey
        StringBuilder quadkey               = new StringBuilder();
        // loop over levels
        for (int i = level+1; i > 0; i--) {
            char digit                      = '0';
            int mask                        = 1 << (i - 1);
            if ((tileX & mask) != 0) {
                digit++;
            }
            if ((tileY & mask) != 0) {
                digit++;
                digit++;
            }
            // add to quadkey
            quadkey.append(digit);
        }
        // return quadkey
        return quadkey.toString();
    }

    /**
     * Get tile indices in x / y from tile quadkey.
     * 
     * @param quadkey
     *                 the quadkey
     */
    public int[] getTileXYfromTileQuadkey(String quadkey) {
        int[] tileXY                        = new int[2];
        // 0-based level index
        int level                           = quadkey.length();
        for (int i = level; i > 0; i--) {
            int mask = 1 << (i - 1);
            switch (quadkey.charAt(level - i)) {
            case '0':
                break;
            case '1':
                tileXY[0]                  |= mask;
                break;
            case '2':
                tileXY[1]                  |= mask;
                break;
            case '3':
                tileXY[0]                  |= mask;
                tileXY[1]                  |= mask;
                break;
            default:
                System.out.println(">>>ERROR:");
                throw new IllegalArgumentException("Invalid quadkey digit sequence.");
            }
        }
        return tileXY;
    }

    /**
     * Print map with labeled tiles.
     * 
     * @param level
     *                 the level
     * @param tiles
     *                 the relevant tiles
     * @param printType
     *                 the print type (e.g., png or text)
     * @param fileName
     *                 the output filename
     */
    public void printMap(int level, int[] tiles, String printType, String fileName) {
        // get number of tiles in x/y
        int numTilesX                      = (int) Math.pow(2, level+1);
        int numTilesY                      = (int) Math.pow(2, level  );
        // print labeled cells
        if (printType.equalsIgnoreCase("text")) {
            if (level > 4) {
                System.out.println(">>>WARNING: Writing to console won't give readable output. Skipping..");
                return;
            }
            String header                  = new String(new char[(numTilesX+1)*2-1]).replace('\0', '-');
            for (int idxY = numTilesY-1; idxY >= 0; idxY--) {
                System.out.println(header);
                for (int idxX = 0; idxX < numTilesX; idxX++) {
                    int tileID             = getTileNumberfromTileXY(level, idxX, idxY);
                    // print label
                    // note: if check is quite inefficient..
                    if (Arrays.stream(tiles).anyMatch(i -> i == tileID)) {
                        System.out.print("|X");
                    } else {
                        System.out.print("| ");
                    }
                }
                System.out.println("|");
            }
            System.out.println(header);
        } else if (printType.equalsIgnoreCase("png")) {
            if (level > 13) {
                System.out.println(">>>WARNING: Required heap space is "+((long)numTilesX*(long)numTilesY*(long)4/(long)1024/(long)1024)+" MB.");
                System.out.println(">>>WARNING: Writing to buffered image may fail due to limited heap space. Skipping..");
                return;
            }
            // create image using default value for type int: 0
            BufferedImage image            = new BufferedImage(numTilesX, numTilesY, BufferedImage.TYPE_INT_RGB);
            for (int idx = 0; idx < tiles.length; idx++) {
                int[] tileXY               = getTileXYfromTileNumber(level, tiles[idx]);
                image.setRGB(tileXY[0], numTilesY-1-tileXY[1], -16000);
            }
            File ImageFile                 = new File(System.getProperty("user.home"), fileName+".png");
            try {
                ImageIO.write(image, "png", ImageFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return;
    }
    
    /**
     * Compute a hash map from tile numbers.
     * 
     * @param level
     *              the level
     * @param tileNumbers
     *              the tileNumbers
     * @return
     */
    public Map<Integer, List<Integer>> tileNumbersToHM(int level, int[] tileNumbers){
        // get the master tile number 0
        NDSTile masterTile                 = new NDSTile(0, 0);
        // create hashmap of (key, value) pairs, where key is tileY and val is tileX
        //         note: val may contain multiple values
        Map<Integer, List<Integer>> tileHM = new HashMap<Integer, List<Integer>>();
        for (int ti = 0; ti < tileNumbers.length; ti++) {
            int[] tileXY                   = getTileXYfromTileNumber(level, tileNumbers[ti]);
            int key                        = tileXY[1];
            int newVal                     = tileXY[0];
            // if key already exists, add value to sorted list; else create new list
            if (tileHM.containsKey(key)) {
                List<Integer> prevList     = tileHM.get(key);
                prevList.add(newVal);
                Collections.sort(prevList);
                tileHM.put(key, prevList);
            } else {
                List<Integer> newList      = new ArrayList<Integer>();
                newList.add(newVal);
                tileHM.put(key, newList);
            }
        }
        return tileHM;
    }
    
    /**
     * Compute tile numbers from hash map.
     * 
     * @param level
     *              the level
     * @param hm
     *              the hash map
     * @return
     */
    public int[] hmToTileNumbers(int level, Map<Integer, List<Integer>> hm) {
        NDSTile masterTile                 = new NDSTile(0, 0);
        int numVals                        = getNumberOfValuesHM(hm);
        int[] filledTileIDs                = new int[numVals];
        int idx                            = 0;
        for (Map.Entry<Integer, List<Integer>> entry : hm.entrySet()) {
            int key                        = entry.getKey();
            List<Integer> currList         = entry.getValue();
            for (int idx0 = 0; idx0 < currList.size(); idx0++) {
                filledTileIDs[idx]         = getTileNumberfromTileXY(level, currList.get(idx0), key);
                idx++;
            }
        }
        return filledTileIDs;
    }
    
    
    private int getNumberOfValuesHM(Map<Integer, List<Integer>> hm) {
        int numVals                        = 0;
        for (Map.Entry<Integer, List<Integer>> entry : hm.entrySet()) {
            numVals                        = numVals + entry.getValue().size();
        }
        return numVals;
    }

    /**
     * Checks if the current Tile contains a certain coordinate.
     *
     * @param c
     *              the coordinate
     * @return true, if successful
     */
    public boolean contains(NDSCoordinate c) {
        /*
         * Checks containment via verifying if the coordinates' tile number for the current tile level matches.
         * 
         * The tile number is identical to the (2*level+1) most-significant bits of
         * the Morton code of the south-west corner of the tile.
         */
        return tileNumber == (int) (c.getMortonCode() >> 32 + (MAX_LEVEL - level) * 2);
    }

    /**
     * Returns the packed Tile ID for this tile. Contains the level and (partial) morton code (bitwise)
     * 
     * @see NDSFormatSpecification: 7.3.3 Generating Packed Tile IDs
     *
     * @return
     */
    public int packedId() {
        return tileNumber + (1 << (16 + level));
    }

    /**
     * Returns the center of this tile as NDSCoordinate
     *
     * @return NDSCoordinate The center of this tile
     */
    public NDSCoordinate getCenter() {
        if (center == null) {
            if (level == 0) {
                return tileNumber == 0 ? new NDSCoordinate(NDSCoordinate.MAX_LONGITUDE / 2, 0)
                                : new NDSCoordinate(NDSCoordinate.MIN_LONGITUDE / 2, 0);
            }
            NDSCoordinate sw = new NDSCoordinate(southWestAsMorton());
            // Same computation as for bounding box, but for the next lower level
            int clat = (int) (sw.latitude + Math.floor(NDSCoordinate.LATITUDE_RANGE / (1L << level + 1))) + (sw.latitude < 0 ? 1 : 0);
            int clon = (int) (sw.longitude + Math.floor(NDSCoordinate.LONGITUDE_RANGE / (1L << level + 2))) + (sw.longitude < 0 ? 1 : 0);
            center = new NDSCoordinate(clon, clat);
        }
        return center;
    }
    
    /*
     * Returns the corners of the tile
     * 
     * @param masterTile
     *              the masterTile
     */
    public NDSCoordinate[] getCorners() {
        NDSBBox bb = getBBox();
        return new NDSCoordinate[] {bb.southWest(), bb.southEast(), bb.northEast(), bb.northWest()};
    }
    
    /*
     * Return the child tile numbers
     */
    public int[] getChildTileNumbers() {
        int id0                             = (tileNumber << 2);
        return new int[] {id0, id0+1, id0+3, id0+2};
    }
    
    public int getChildTileNumberSouthWest() {
        return (tileNumber << 2);
    }
    
    public int getChildTileNumberSouthEast() {
        return (tileNumber << 2) + 1;
    }
    
    public int getChildTileNumberNorthEast() {
        return (tileNumber << 2) + 3;
    }
    
    public int getChildTileNumberNorthWest() {
        return (tileNumber << 2) + 2;
    }

    /**
     * Creates a bounding box for the current tile.
     *
     * @see NDSFormatSpecification: 7.3.3 Generating Packed Tile IDs
     * 
     * @return
     */
    public NDSBBox getBBox() {
        /*
         * For level 0 there are two tiles.
         */
        if (level == 0) {
            return tileNumber == 0 ? NDSBBox.EAST_HEMISPHERE : NDSBBox.WEST_HEMISPHERE;
        }
        long southWestCornerMorton = southWestAsMorton();
        NDSCoordinate sw = new NDSCoordinate(southWestCornerMorton);
        int north = (int) (sw.latitude + Math.floor(NDSCoordinate.LATITUDE_RANGE / (1L << level))) + (sw.latitude < 0 ? 1 : 0);
        int east = (int) (sw.longitude + Math.floor(NDSCoordinate.LONGITUDE_RANGE / (1L << level + 1))) + (sw.longitude < 0 ? 1 : 0);
        return new NDSBBox(north, east, sw.latitude, sw.longitude);
    }

    /**
     * Computes a GeoJSON representation of the NDS Tile as GeoJSON "Polygon" feature.
     *
     * @return String
     */
    public String toGeoJSON() {
        return getBBox().toWGS84().toGeoJSON();
    }

    private long southWestAsMorton() {
        int shift = 32 + (MAX_LEVEL - level) * 2;
        return (long) tileNumber << shift;
    }

    private int extractLevel(int packedId) {
        for (int lvl = MAX_LEVEL; lvl > -1; lvl--) {
            int lvl_bit = 1 << 16 + lvl;
            if ((packedId & lvl_bit) > 0) {
                return lvl;
            }
            // The 32nd bit can not be checked by java's native integer, as the sign bit is hidden
            if (packedId < 0 && lvl == MAX_LEVEL)
                return MAX_LEVEL;
        }
        return -1;
    }
}
