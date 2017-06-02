package com.daniel0x7cc.chatify.utils;

import android.graphics.Bitmap;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Custom class which includes the several static functions and static variables.
 */
public class GlobalUtils {

    public static final int INPUT_AREA = 8;
    public static final int INPUT_PRICE = 9;
    public static String POST_TYPE_HAS = "sells";
    public static String POST_TYPE_NEED = "buy";
    public static String ACTION_RECEIVED_PUSH = "received_push";
    public static Locale locale;

    public static boolean isLandscape(Bitmap source) {
        return source.getWidth() > source.getHeight();
    }

    public static String getScore(String value) {
        try {
            float distance = Float.valueOf(value);
            return String.format("%,.1f", distance);
        } catch (Exception e) {
            return value;
        }
    }

    public static String addThousandsSeparator(String value) {
        try {
            DecimalFormat df = new DecimalFormat("#,###.##", new DecimalFormatSymbols(new Locale("pt", "BR")));
            DecimalFormat dfnd = new DecimalFormat("#,###", new DecimalFormatSymbols(new Locale("pt", "BR")));

            String replaceDecimal = value.replace(".", ",");
            String v = replaceDecimal.replace(String.valueOf(df.getDecimalFormatSymbols().getGroupingSeparator()), "");
            BigDecimal num = new BigDecimal(df.parse(v).doubleValue());
            if (replaceDecimal.contains(String.valueOf(df.getDecimalFormatSymbols().getDecimalSeparator()))) {
                return df.format(num.doubleValue());
            } else {
                return dfnd.format(num.doubleValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return value;
        }
    }

    public static String formatAmount(String value) {
        try {
            DecimalFormat df = new DecimalFormat("#,###.##", new DecimalFormatSymbols(new Locale("pt", "BR")));
            DecimalFormat dfnd = new DecimalFormat("#,###", new DecimalFormatSymbols(new Locale("pt", "BR")));

            String replaceDecimal = value.replace(".", ",");
            String v = replaceDecimal.replace(String.valueOf(df.getDecimalFormatSymbols().getGroupingSeparator()), "");
            BigDecimal num = new BigDecimal(df.parse(v).doubleValue());
            if (replaceDecimal.contains(String.valueOf(df.getDecimalFormatSymbols().getDecimalSeparator()))) {
                return df.format(num.doubleValue());
            } else {
                return dfnd.format(num.doubleValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return value;
        }
    }

    public static boolean isValidEmail(String target) {
        if (target == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public static String getFirstWord(String text) {
        if (text == null || text.trim().length() <= 0 || text.indexOf(' ') <= 0) {
            return text;
        } else {
            int index = text.indexOf(' ');
            return text.substring(0, index);
        }
    }
}
