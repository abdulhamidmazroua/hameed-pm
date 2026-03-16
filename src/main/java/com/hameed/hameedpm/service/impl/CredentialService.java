package com.hameed.hameedpm.service.impl;

import com.hameed.hameedpm.model.Credential;
import com.hameed.hameedpm.service.ICredentialService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CredentialService implements ICredentialService {

    @Override
    public void addCredential(String serviceName, String username, char[] password, Map<String, String> additionalInfo) {

    }

    @Override
    public List<Credential> listCredentials() {

        return null;

    }

    @Override
    public Optional<Credential> getCredentialByServiceName(String service) {
        return Optional.empty();
    }

    @Override
    public boolean updateCredential(String serviceName, Credential updatedCredential) {
        return false;
    }
}
