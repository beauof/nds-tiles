package de.rondiplomatico.nds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NDSHashmap allows to store tileY - tileX pairs in a convenient format,
 * where tileY is the key and tileX are the value.
 * This is convenient because each tileY key has a (sorted) number of tileX value(s)
 * and makes several tasks straighforward, such as:
 * - drawing a map
 * - applying flood-fill algorithm to get map tiles covered by polygon
 * - etc.
 * 
 * No warranties for correctness, use at own risk.
 * 
 * @author Andreas Hessenthaler
 * @since 28.02.2020
 */
public class NDSHashmap {
    
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
            int[] tileXY                   = masterTile.getTileXYfromTileNumber(level, tileNumbers[ti]);
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
                filledTileIDs[idx]         = masterTile.getTileNumberFromTileXY(level, currList.get(idx0), key);
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

}
