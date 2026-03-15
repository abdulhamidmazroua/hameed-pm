package com.hameed.hameedpm.service;

import com.hameed.hameedpm.model.Credential;

import java.util.List;
import java.util.Map;

public interface ICredentialService {
    void addCredential(String serviceName, String username, char[] password, Map<String, String> additionalInfo);
    List<Credential> listCredentials();
    Credential getCredentialByServiceName(String service);

}
