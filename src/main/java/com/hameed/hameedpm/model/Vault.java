package com.hameed.hameedpm.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Vault {

    private String name;
    private byte[] signingKey;
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

    public byte[] getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(byte[] signingKey) {
        this.signingKey = signingKey;
    }
}
