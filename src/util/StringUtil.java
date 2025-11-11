package util;

import java.util.regex.Pattern;

public class StringUtil {

    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    public static void requireSafeName(String value, String field) {
        if (value == null || !SAFE_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " contains invalid characters");
        }
    }

}
