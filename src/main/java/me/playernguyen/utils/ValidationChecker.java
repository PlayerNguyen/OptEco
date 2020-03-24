package me.playernguyen.utils;


import java.util.regex.Pattern;

public class ValidationChecker {

    public static boolean isNumber(String s) {
        if (s == null) {
            return false;
        }
        return Pattern.compile("-?\\d+(\\.\\d+)?").matcher(s).matches();
    }


}
