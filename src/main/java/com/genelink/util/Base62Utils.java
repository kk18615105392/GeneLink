package com.genelink.util;

public final class Base62Utils {

    private static final String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = CHARACTERS.length();

    private Base62Utils() {
    }

    public static String encode(long num) {
        if (num == 0) {
            return String.valueOf(CHARACTERS.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(CHARACTERS.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String str) {
        long num = 0;
        for (int i = 0; i < str.length(); i++) {
            num = num * BASE + CHARACTERS.indexOf(str.charAt(i));
        }
        return num;
    }
}
