package com.dragonminez.client.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberFormattingUtil {
    private NumberFormattingUtil() {
    }

    private static final NumberFormat NUMBER_FORMATTER = NumberFormat.getInstance(Locale.US);
    private static final DecimalFormat ONE_DECIMAL_FORMATTER = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat TWO_DECIMAL_FORMATTER = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat SCIENTIFIC_FORMATTER = new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat FULL_TPS_FORMATTER = new DecimalFormat("#,##0.######", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat COMPACT_NUMBER_FORMATTER = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));

    public static boolean shouldUseCompactForm(double bp) {
        return bp > 999L;
    }

    public static String formatLargeNumber(double largeNumber) {
        if (!shouldUseCompactForm(largeNumber)) return TWO_DECIMAL_FORMATTER.format(largeNumber);

        final String[] suffixes = {"K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};
        final double[] scales = {1e3, 1e6, 1e9, 1e12, 1e15, 1e18, 1e21, 1e24, 1e27, 1e30, 1e33};
        int i = scales.length - 1;
        while (i > 0 && largeNumber < scales[i]) i--;
        return COMPACT_NUMBER_FORMATTER.format(largeNumber / scales[i]) + suffixes[i];
    }

    public static boolean shouldUseScientificForm(float value) {
        if (!Float.isFinite(value)) return false;
        return Math.floor(Math.abs(value)) >= 999_999_999d;
    }

    public static String formatScientificNumber(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return String.valueOf(value);
        return shouldUseScientificForm(value) ? SCIENTIFIC_FORMATTER.format(value) : FULL_TPS_FORMATTER.format(value);
    }

    public static String formatNumber(double value) {
        return NUMBER_FORMATTER.format(value);
    }

    public static String formatUpToOneDecimal(double value) {
        return ONE_DECIMAL_FORMATTER.format(value);
    }

    public static String formatUpToTwoDecimals(double value) {
        return TWO_DECIMAL_FORMATTER.format(value);
    }

    public static String formatFullTps(double value) {
        return FULL_TPS_FORMATTER.format(value);
    }
}
