package de.rondiplomatico.nds;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.vividsolutions.jts.geom.OctagonalEnvelope;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Tests for the NDSEnvelope class.
 * 
 * @author Andreas Hessenthaler
 * @since 28.02.2020
 */
@Getter
@ToString
@EqualsAndHashCode
public class NDSEnvelopeTest {

    private final double eps = 1E-7;
    
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
}
