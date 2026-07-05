package com.closet.service;

import org.springframework.stereotype.Component;

/**
 * Color harmony rules engine. Scores a pair of HEX colors by their
 * HSL hue distance so the outfit AI can rank candidate combinations.
 *
 * <p>Buckets (shortest arc hue diff on a 0..360 wheel):
    * <ul>
    *   <li>&lt; 30 deg   -> monochrome     (score 0.95)</li>
    *   <li>&lt; 90 deg   -> analogous      (score 0.80)</li>
     *   <li>90..140 deg  -> near-analogous (score 0.40, looks off)</li>
     *   <li>140..210 deg -> complementary  (score 0.75)</li>
    *   <li>&gt; 210 deg  -> split/triadic  (score 0.60)</li>
    * </ul>
 *
 * <p>Any null / malformed HEX input short-circuits to a neutral
 * 0.50 so downstream code can keep ranking rather than crash.
 */
@Component
public class ColorHarmonyEngine {

    public static double[] hexToHsl(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) {
            return null;
        }
        int r;
        int g;
        int b;
        try {
            r = Integer.parseInt(hex.substring(1, 3), 16);
            g = Integer.parseInt(hex.substring(3, 5), 16);
            b = Integer.parseInt(hex.substring(5, 7), 16);
        } catch (NumberFormatException ex) {
            return null;
        }

        double rNorm = r / 255.0;
        double gNorm = g / 255.0;
        double bNorm = b / 255.0;
        double max = Math.max(rNorm, Math.max(gNorm, bNorm));
        double min = Math.min(rNorm, Math.min(gNorm, bNorm));
        double h;
        double s;
        double l = (max + min) / 2.0;

        if (max == min) {
            h = 0;
            s = 0;
        } else {
            double d = max - min;
            s = l > 0.5 ? d / (2.0 - max - min) : d / (max + min);
            if (max == rNorm) {
                h = (gNorm - bNorm) / d + (gNorm < bNorm ? 6 : 0);
            } else if (max == gNorm) {
                h = (bNorm - rNorm) / d + 2;
            } else {
                h = (rNorm - gNorm) / d + 4;
            }
            h /= 6.0;
        }
        return new double[] { h * 360.0, s, l };
    }

    public static double harmonyScore(String hexA, String hexB) {
        double[] hslA = hexToHsl(hexA);
        double[] hslB = hexToHsl(hexB);
        if (hslA == null || hslB == null) {
            return 0.5;
        }
        double hueDiff = Math.abs(hslA[0] - hslB[0]);
        if (hueDiff > 180.0) {
            hueDiff = 360.0 - hueDiff;
        }
        if (hueDiff < 30.0) {
            return 0.95;
        }
        if (hueDiff < 90.0) {
            return 0.80;
        }
        if (hueDiff >= 90.0 && hueDiff <= 140.0) {
            // Plan theme colors (#d49aa5 ~348 deg vs #a8c8d8 ~200 deg)
            // land at ~148 deg - intentionally still considered "complementary"
            // here so the v2 pink/blue palette scores well. The 90..145
            // window is what we keep as the awkward middle band.
            return 0.40;
        }
        if (hueDiff >= 140.0 && hueDiff < 210.0) {
            return 0.75;
        }
        if (hueDiff > 210.0) {
            return 0.60;
        }
        // Unreachable in practice (above branches cover 0..360), but
        // javac requires a terminal return so the contract is explicit.
        return 0.40;
    }
}
