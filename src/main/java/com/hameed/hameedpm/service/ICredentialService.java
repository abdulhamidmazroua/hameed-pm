package com.hameed.hameedpm.service;

import com.hameed.hameedpm.model.Credential;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ICredentialService {
    void addCredential(String serviceName, String username, char[] password, Map<String, String> additionalInfo);
    List<Credential> listCredentials();
    Optional<Credential> getCredentialByServiceName(String service);
    boolean updateCredential(String serviceName, Credential updatedCredential);

}
