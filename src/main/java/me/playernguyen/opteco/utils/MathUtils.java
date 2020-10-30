package me.playernguyen.opteco.utils;


import java.util.regex.Pattern;

public class MathUtils {

    public static boolean isNotNumber(String s) {
        if (s == null) {
            return false;
        }
        return !Pattern.compile("-?\\d+(\\.\\d+)?").matcher(s).matches();
    }

    public static boolean isInteger(String str) {
        try {
            int i = Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


}
