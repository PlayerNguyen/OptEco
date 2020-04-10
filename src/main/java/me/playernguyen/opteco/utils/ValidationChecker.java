package me.playernguyen.opteco.utils;


import java.util.regex.Pattern;

public class ValidationChecker {

    public static boolean isNotNumber(String s) {
        if (s == null) {
            return false;
        }
        return !Pattern.compile("-?\\d+(\\.\\d+)?").matcher(s).matches();
    }


}
