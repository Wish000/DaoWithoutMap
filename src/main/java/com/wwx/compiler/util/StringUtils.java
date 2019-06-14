package com.wwx.compiler.util;

import javax.validation.constraints.NotNull;

public class StringUtils {
    private StringUtils() {
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static String camelCase2_(@NotNull String s) {
        StringBuilder sb = new StringBuilder();
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= 'A' && c <= 'Z' && i > 0) {
                sb.append("_");
            }
            sb.append(c);
        }
        return sb.toString().toUpperCase();
    }

    public static String toCamelCase(@NotNull String s) {
        StringBuilder sb = new StringBuilder();
        char[] chars = s.toLowerCase().toCharArray();
        int k = -1;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '_') {
                k = i + 1;
            } else if (k == i) {
                c -= 'a' - 'A';
                sb.append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String lowerFirstLetter(@NotNull String s) {
        if (s.length() <= 1) {
            return s.toLowerCase();
        }
        StringBuilder sb = new StringBuilder();
        char fc = s.charAt(0);
        if (fc > 'A' && fc <= 'Z') {
            fc += 'a' - 'A';
        }
        sb.append(fc).append(s.substring(1));
        return sb.toString();
    }
}
