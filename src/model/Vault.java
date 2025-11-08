package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Vault {

    private String name;
    private List<Credential> credentials = new ArrayList<>();

    public Vault() {}

    public Vault(String name, List<Credential> credentials) {
        this.name = name;
        this.credentials = credentials;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Credential> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<Credential> credentials) {
        this.credentials = credentials;
    }

    public void add(Credential credential) {
        if (credentials.isEmpty())
            credentials = new ArrayList<>();
        credentials.add(credential);
    }

    public boolean remove(String serviceName) {
        for (Credential credential : credentials) {
            if (credential.getServiceName().equalsIgnoreCase(serviceName))
                return credentials.remove(credential);
        }
        return false;
    }

    public Optional<Credential> find(String serviceName) {
        for (Credential credential : credentials) {
            if (credential.getServiceName().equalsIgnoreCase(serviceName)) {
                return Optional.of(credential);
            }
        }

        return Optional.empty();
    }
}
