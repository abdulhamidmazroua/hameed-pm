package enums;

import util.ColorUtil;

public enum Command {
    ADD,
    LIST,
    GET,
    UPDATE,
    DELETE,
    HELP,
    CLEAR,
    EXIT;

    @Override
    public String toString() {
       return this.name();
    }

    public String usage() {
        switch (this) {
            case ADD -> {
                return ColorUtil.boldCyan("add");
            }
            case LIST -> {
                return ColorUtil.boldCyan("list");
            }
            case GET -> {
                return ColorUtil.boldCyan("get")
                        + " " + ColorUtil.magenta("<service-name>");
            }
            case UPDATE -> {
                return ColorUtil.boldRed("update")
                        + " " + ColorUtil.magenta("<service-name>")
                        + " " + ColorUtil.green("<field-name>")
                        + " " + ColorUtil.green("<new-value>");
            }
            case DELETE -> {
                return ColorUtil.boldRed("delete")
                        + " " + ColorUtil.magenta("<service-name>");
            }
            case HELP -> {
                return ColorUtil.boldCyan("help");
            }
            case CLEAR -> {
                return ColorUtil.boldCyan("clear")
                        + ColorUtil.dim("   (clears screen)");
            }
            case EXIT -> {
                return ColorUtil.boldCyan("exit")
                        + ColorUtil.dim("   (quit program)");
            }
            default -> {
                return ColorUtil.red("Invalid command.");
            }
        }
    }

}
