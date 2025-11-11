package util;

public class ColorUtil {
    // Reset
    public static final String RESET = "\u001B[0m";

    // Bold
    public static final String BOLD = "\u001B[1m";

    // Basic colors
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String DIM = "\u001B[2m";

    // Helpers
    public static String red(String text) { return RED + text + RESET; }
    public static String green(String text) { return GREEN + text + RESET; }
    public static String yellow(String text) { return YELLOW + text + RESET; }
    public static String blue(String text) { return BLUE + text + RESET; }
    public static String magenta(String text) { return MAGENTA + text + RESET; }
    public static String cyan(String text) { return CYAN + text + RESET; }
    public static String white(String text) { return WHITE + text + RESET; }
    public static String dim(String text) { return DIM + text + RESET; }

    public static String bold(String text) { return BOLD + text + RESET; }

    // Combined examples
    public static String boldRed(String text) { return BOLD + RED + text + RESET; }
    public static String boldGreen(String text) { return BOLD + GREEN + text + RESET; }
    public static String boldBlue(String text) { return BOLD + BLUE + text + RESET; }
    public static String boldCyan(String text) { return BOLD + CYAN + text + RESET; }
}


