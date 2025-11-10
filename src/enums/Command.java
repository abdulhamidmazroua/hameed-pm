package enums;

public enum Command {
    ADD,
    LIST,
    GET,
    DELETE,
    HELP,
    CLEAR,
    EXIT;

    @Override
    public String toString() {
        return this.name();
    }
}
