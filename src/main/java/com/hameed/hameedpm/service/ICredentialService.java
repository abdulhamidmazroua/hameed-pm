package com.hameed.hameedpm.service;

import com.hameed.hameedpm.model.Credential;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ICredentialService {
    void addCredential(Credential credential) throws Exception;
    List<Credential> listCredentials();
    Optional<Credential> getCredentialByServiceName(String serviceName);
    void updateCredential(String serviceName, Credential updatedCredential) throws Exception;
    void deleteCredential(String serviceName) throws Exception;

}
