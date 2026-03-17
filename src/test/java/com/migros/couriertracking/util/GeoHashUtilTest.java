package com.migros.couriertracking.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GeoHashUtilTest {

    // Ataşehir MMM Migros coordinates
    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;

    @Test
    @DisplayName("Encode produces correct length for given precision")
    void encodePrecisionControlsLength() {
        assertEquals(5, GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG, 5).length());
        assertEquals(7, GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG, 7).length());
        assertEquals(9, GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG, 9).length());
    }

    @Test
    @DisplayName("Same coordinates always produce the same geohash")
    void encodeDeterministic() {
        String hash1 = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG);
        String hash2 = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG);
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Default encode uses precision 7")
    void defaultPrecisionIsSeven() {
        String hash = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG);
        assertEquals(7, hash.length());
    }

    @Test
    @DisplayName("Nearby points (< 50m apart) share at least 6-char prefix")
    void nearbyPointsSharePrefix() {
        // ~50m north of Ataşehir Migros
        String hash1 = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG, 7);
        String hash2 = GeoHashUtil.encode(ATASEHIR_LAT + 0.0004, ATASEHIR_LNG, 7);
        // Should share at least first 6 characters
        assertEquals(hash1.substring(0, 6), hash2.substring(0, 6));
    }

    @Test
    @DisplayName("Distant points have different geohashes at low precision")
    void distantPointsDifferEarly() {
        // Ataşehir vs Beylikdüzü (~40km apart)
        String atasehir = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG, 4);
        String beylikduzu = GeoHashUtil.encode(41.0066851, 28.6552262, 4);
        assertNotEquals(atasehir, beylikduzu);
    }

    @Test
    @DisplayName("getSelfAndNeighbors returns 9 cells including self")
    void selfAndNeighborsReturnsNineIncludingSelf() {
        String hash = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG);
        String[] cells = GeoHashUtil.getSelfAndNeighbors(hash);
        assertEquals(9, cells.length);
        assertEquals(hash, cells[0]);
    }

    @Test
    @DisplayName("All neighbor cells have same length as input")
    void neighborsSameLength() {
        String hash = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG);
        String[] cells = GeoHashUtil.getSelfAndNeighbors(hash);
        for (int i = 1; i < cells.length; i++) {
            assertEquals(hash.length(), cells[i].length());
        }
    }

    @Test
    @DisplayName("All 9 cells are unique")
    void selfAndNeighborsAreUnique() {
        String hash = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG);
        String[] cells = GeoHashUtil.getSelfAndNeighbors(hash);
        Set<String> unique = Arrays.stream(cells).collect(Collectors.toSet());
        assertEquals(9, unique.size());
    }

    @Test
    @DisplayName("Boundary coordinates encode without error")
    void boundaryCoordinates() {
        assertDoesNotThrow(() -> GeoHashUtil.encode(0, 0));
        assertDoesNotThrow(() -> GeoHashUtil.encode(90, 180));
        assertDoesNotThrow(() -> GeoHashUtil.encode(-90, -180));
    }

    @Test
    @DisplayName("Point 100m away is within self+neighbors cell set")
    void pointWithin100mIsInNeighborhood() {
        String storeHash = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG);
        String[] cells = GeoHashUtil.getSelfAndNeighbors(storeHash);
        Set<String> cellSet = new HashSet<>(Arrays.asList(cells));

        // ~100m north
        String nearbyHash = GeoHashUtil.encode(ATASEHIR_LAT + 0.0009, ATASEHIR_LNG);
        assertTrue(cellSet.contains(nearbyHash),
                "Point ~100m away should be in the 9-cell neighborhood");
    }

    @Test
    @DisplayName("Geohash only contains valid base32 characters")
    void encodedHashContainsValidChars() {
        String validChars = "0123456789bcdefghjkmnpqrstuvwxyz";
        String hash = GeoHashUtil.encode(ATASEHIR_LAT, ATASEHIR_LNG, 9);
        for (char c : hash.toCharArray()) {
            assertTrue(validChars.indexOf(c) >= 0, "Invalid char: " + c);
        }
    }

    @Test
    @DisplayName("Invalid geohash characters are rejected during decode")
    void invalidGeohashCharacterIsRejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> GeoHashUtil.getSelfAndNeighbors("u4pruy!")
        );

        assertEquals("Invalid geohash character: !", ex.getMessage());
    }
}
