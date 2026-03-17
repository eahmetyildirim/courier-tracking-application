package com.migros.couriertracking.util;

import com.migros.couriertracking.model.Store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.migros.couriertracking.constant.Constants.*;

/**
 * Geohash encoding, decoding, and neighbor computation utility.
 * Provides O(1) spatial indexing for proximity queries by dividing the world
 * into a hierarchical grid of cells identified by base-32 strings.
 *
 * <p>Higher precision yields smaller cells. Default precision 7 produces
 * approximately 150m &times; 150m cells, suitable for store proximity detection.</p>
 *
 * <p>This class is stateless and thread-safe. All methods are static.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Geohash">Geohash (Wikipedia)</a>
 */
public final class GeoHashUtil {

    public static final int DEFAULT_PRECISION = 7;

    private static final char[] BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz".toCharArray();
    private static final int[] BASE32_DECODE = new int[128];
    private static final int BASE32_BITS = 5;
    private static final int MAX_BIT_OFFSET = BASE32_BITS - 1;
    private static final int NEIGHBOR_COUNT = 8;
    private static final int CELL_COUNT = 9;

    static {
        Arrays.fill(BASE32_DECODE, -1);
        for (int i = 0; i < BASE32.length; i++) {
            BASE32_DECODE[BASE32[i]] = i;
        }
    }

    private GeoHashUtil() {
    }

    /**
     * Encode latitude/longitude to a geohash string with the given precision.
     */
    public static String encode(double lat, double lng, int precision) {
        double latMin = MIN_LATITUDE, latMax = MAX_LATITUDE;
        double lngMin = MIN_LONGITUDE, lngMax = MAX_LONGITUDE;
        boolean isLng = true;
        int bit = 0;
        int charIndex = 0;
        StringBuilder hash = new StringBuilder(precision);

        while (hash.length() < precision) {
            double mid;
            if (isLng) {
                mid = (lngMin + lngMax) / 2;
                if (lng >= mid) {
                    charIndex |= (1 << (MAX_BIT_OFFSET - bit));
                    lngMin = mid;
                } else {
                    lngMax = mid;
                }
            } else {
                mid = (latMin + latMax) / 2;
                if (lat >= mid) {
                    charIndex |= (1 << (MAX_BIT_OFFSET - bit));
                    latMin = mid;
                } else {
                    latMax = mid;
                }
            }
            isLng = !isLng;
            bit++;

            if (bit == BASE32_BITS) {
                hash.append(BASE32[charIndex]);
                bit = 0;
                charIndex = 0;
            }
        }

        return hash.toString();
    }

    /**
     * Encode with default precision (7 ≈ 150m × 150m cells).
     */
    public static String encode(double lat, double lng) {
        return encode(lat, lng, DEFAULT_PRECISION);
    }

    /**
     * Decode a geohash to its bounding box: [latMin, latMax, lngMin, lngMax].
     */
    private static double[] decode(String geohash) {
        double latMin = MIN_LATITUDE, latMax = MAX_LATITUDE;
        double lngMin = MIN_LONGITUDE, lngMax = MAX_LONGITUDE;
        boolean isLng = true;

        for (int i = 0; i < geohash.length(); i++) {
            int charValue = base32Index(geohash.charAt(i));
            for (int bit = MAX_BIT_OFFSET; bit >= 0; bit--) {
                if (isLng) {
                    double mid = (lngMin + lngMax) / 2;
                    if (((charValue >> bit) & 1) == 1) {
                        lngMin = mid;
                    } else {
                        lngMax = mid;
                    }
                } else {
                    double mid = (latMin + latMax) / 2;
                    if (((charValue >> bit) & 1) == 1) {
                        latMin = mid;
                    } else {
                        latMax = mid;
                    }
                }
                isLng = !isLng;
            }
        }

        return new double[]{latMin, latMax, lngMin, lngMax};
    }

    /**
     * Get the 8 neighboring geohash cells using decode-offset-encode.
     * Directions: N, NE, E, SE, S, SW, W, NW
     */
    private static String[] getNeighbors(String geohash) {
        double[] bounds = decode(geohash);
        double latHeight = bounds[1] - bounds[0];
        double lngWidth = bounds[3] - bounds[2];
        double centerLat = (bounds[0] + bounds[1]) / 2;
        double centerLng = (bounds[2] + bounds[3]) / 2;
        int precision = geohash.length();

        return new String[]{
                encode(centerLat + latHeight, centerLng, precision),              // N
                encode(centerLat + latHeight, centerLng + lngWidth, precision),   // NE
                encode(centerLat, centerLng + lngWidth, precision),              // E
                encode(centerLat - latHeight, centerLng + lngWidth, precision),   // SE
                encode(centerLat - latHeight, centerLng, precision),              // S
                encode(centerLat - latHeight, centerLng - lngWidth, precision),   // SW
                encode(centerLat, centerLng - lngWidth, precision),              // W
                encode(centerLat + latHeight, centerLng - lngWidth, precision)    // NW
        };
    }

    /**
     * Get self + 8 neighboring geohash cells (9 total).
     */
    public static String[] getSelfAndNeighbors(String geohash) {
        String[] neighbors = getNeighbors(geohash);
        String[] result = new String[CELL_COUNT];
        result[0] = geohash;
        System.arraycopy(neighbors, 0, result, 1, NEIGHBOR_COUNT);
        return result;
    }

    /**
     * Build a geohash-based spatial index from a list of stores.
     * Each store is mapped to its geohash cell for O(1) proximity lookups.
     *
     * @param stores the list of stores to index
     * @return unmodifiable map of geohash → list of stores in that cell
     */
    public static Map<String, List<Store>> buildIndex(List<Store> stores) {
        Map<String, List<Store>> index = new HashMap<>();
        for (Store store : stores) {
            String hash = encode(store.lat(), store.lng());
            index.computeIfAbsent(hash, k -> new ArrayList<>()).add(store);
        }
        index.replaceAll((k, v) -> Collections.unmodifiableList(v));
        return Collections.unmodifiableMap(index);
    }

    private static int base32Index(char c) {
        if (c < BASE32_DECODE.length) {
            int index = BASE32_DECODE[c];
            if (index >= 0) {
                return index;
            }
        }
        throw new IllegalArgumentException("Invalid geohash character: " + c);
    }
}
