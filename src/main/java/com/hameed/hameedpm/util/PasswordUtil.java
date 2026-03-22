package com.hameed.hameedpm.util;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

public class PasswordUtil {

    public static char[] promptPassword(String prompt, LineReader lineReader) {
        String input = lineReader.readLine(prompt, '*');
        return input != null ? input.toCharArray() : new char[0];
    }

    public static boolean validPassword(char[] password) {
        if (password.length < 12) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password) {
            if (Character.isUpperCase(c))      hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c))     hasDigit = true;
            else if ("!@#$%^&*()_+-=[]{}|;':\",.<>/?".indexOf(c) >= 0) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }


}
