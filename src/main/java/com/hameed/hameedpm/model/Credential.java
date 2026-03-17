package com.hameed.hameedpm.model;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Credential {

    private String serviceName;
    private String username;
    private String password;
    private Map<String, String> additionalInfo;

    public Credential() {}

    public Credential(String serviceName, String username, String password, Map<String, String> additionalInfo) {
        this.serviceName = serviceName;
        this.username = username;
        this.password = password;
        this.additionalInfo = additionalInfo;
    }

    // copy constructor
    public Credential(Credential other) {
        this.serviceName = other.serviceName;
        this.username = other.username;
        this.password = other.password;
        // deep copy
        this.additionalInfo = new HashMap<>(other.additionalInfo);
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public boolean addInfo(String key, String value) {
        if (additionalInfo == null) {
            additionalInfo = new LinkedHashMap<>();
        }
        if (additionalInfo.containsKey(key)) return false;
        additionalInfo.put(key, value);
        return true;
    }

    public boolean removeInfo(String key) {
        if (!additionalInfo.containsKey(key)) return false;
        additionalInfo.remove(key);
        return true;
    }

    public boolean updateInfo(String key, String newValue) {
        if (!additionalInfo.containsKey(key)) return false;
        additionalInfo.put(key, newValue);
        return true;
    }
}