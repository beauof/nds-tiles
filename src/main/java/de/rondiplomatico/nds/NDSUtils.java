package de.rondiplomatico.nds;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import de.rondiplomatico.nds.NDSTile;

public class NDSUtils {

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
     public static void printMap(NDSTile masterTile, int level, int[] tiles, String printType, String fileName) {
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
                    int tileID             = masterTile.getTileNumberFromTileXY(level, idxX, idxY);
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
                int[] tileXY               = masterTile.getTileXYfromTileNumber(level, tiles[idx]);
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
}
