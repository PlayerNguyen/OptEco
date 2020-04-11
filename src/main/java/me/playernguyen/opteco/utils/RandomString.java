package me.playernguyen.opteco.utils;

import java.util.Random;

public class RandomString {

    private static final String PATTERN = "abcdeflmngkABCDEFLMNGK1234567890";

    public static String rand(int size) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            builder.append(PATTERN.charAt(new Random().nextInt(PATTERN.length())));
        }
        return builder.toString();
    }

}
