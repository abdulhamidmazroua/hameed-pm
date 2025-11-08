package enums;

public enum Command {
    ADD,
    LIST,
    GET,
    DELETE,
    EXIT;

    @Override
    public String toString() {
        return this.name();
    }
}
