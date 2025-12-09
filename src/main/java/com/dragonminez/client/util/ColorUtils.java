package com.dragonminez.client.util;

public class ColorUtils {

    public static int[] hexToRgb(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new int[]{255, 255, 255};
        }
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            int color = Integer.parseInt(hex, 16);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            return new int[]{255, 255, 255};
        }
    }

    public static String rgbToHex(int r, int g, int b) {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public static int rgbToInt(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }

    public static int[] intToRgb(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return new int[]{r, g, b};
    }

    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0;
        if (delta != 0) {
            if (max == rf) {
                h = 60 * (((gf - bf) / delta) % 6);
            } else if (max == gf) {
                h = 60 * (((bf - rf) / delta) + 2);
            } else {
                h = 60 * (((rf - gf) / delta) + 4);
            }
        }

        if (h < 0) h += 360;

        float s = max == 0 ? 0 : (delta / max) * 100;
        float v = max * 100;

        return new float[]{h, s, v};
    }

    public static int[] hsvToRgb(float h, float s, float v) {
        s = s / 100.0f;
        v = v / 100.0f;

        float c = v * s;
        float x = c * (1 - Math.abs(((h / 60) % 2) - 1));
        float m = v - c;

        float rf, gf, bf;
        if (h >= 0 && h < 60) {
            rf = c; gf = x; bf = 0;
        } else if (h >= 60 && h < 120) {
            rf = x; gf = c; bf = 0;
        } else if (h >= 120 && h < 180) {
            rf = 0; gf = c; bf = x;
        } else if (h >= 180 && h < 240) {
            rf = 0; gf = x; bf = c;
        } else if (h >= 240 && h < 300) {
            rf = x; gf = 0; bf = c;
        } else {
            rf = c; gf = 0; bf = x;
        }

        int r = Math.round((rf + m) * 255);
        int g = Math.round((gf + m) * 255);
        int b = Math.round((bf + m) * 255);

        return new int[]{
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, b))
        };
    }

    public static float[] hexToHsv(String hex) {
        int[] rgb = hexToRgb(hex);
        return rgbToHsv(rgb[0], rgb[1], rgb[2]);
    }

    public static String hsvToHex(float h, float s, float v) {
        int[] rgb = hsvToRgb(h, s, v);
        return rgbToHex(rgb[0], rgb[1], rgb[2]);
    }

    public static int hexToInt(String hex) {
        if (hex == null || hex.isEmpty()) {
            return 0xFFFFFF;
        }
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}

