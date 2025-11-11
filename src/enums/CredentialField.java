package enums;

import java.util.Optional;

public enum CredentialField {
    SERVICE_NAME("service-name"),
    USERNAME("username"),
    PASSWORD("password");

    private final String value;

    CredentialField(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static Optional<CredentialField> fromValue(String fieldName) {
        for(CredentialField credentialField : values()) {
            if (credentialField.toString().equalsIgnoreCase(fieldName))
                return Optional.of(credentialField);
        }
        return Optional.empty();
    }
}
