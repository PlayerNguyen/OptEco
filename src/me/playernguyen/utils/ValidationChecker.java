package me.playernguyen.utils;

import com.sun.istack.internal.NotNull;

import java.util.regex.Pattern;

public class ValidationChecker {

    public static boolean isNumber(@NotNull String s) {
        if (s == null) {
            return false;
        }
        return Pattern.compile("-?\\d+(\\.\\d+)?").matcher(s).matches();
    }


}
