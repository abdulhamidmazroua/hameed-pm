package util;

import java.util.Arrays;
import java.util.regex.Pattern;

public class StringUtil {

    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    public static void requireSafeName(String value, String field) {
        if (value == null || !SAFE_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " contains invalid characters");
        }
    }

    public static void requireValidPassword(char[] password) {
        String pwd = new String(password);
        if (!(pwd.length() >= 12 &&                      // min length
            pwd.matches(".*[A-Z].*") &&               // at least one uppercase
            pwd.matches(".*[a-z].*") &&               // at least one lowercase
            pwd.matches(".*\\d.*") &&                 // at least one digit
            pwd.matches(".*[^A-Za-z0-9].*"))) {         // at least one special char

            throw new IllegalArgumentException("Entered password does not meet requirements: " +
                    "12 characters, at least 1 uppercase, at least 1 lowercase, at least 1 digit, and at least 1 special char");
        }
    }

}
