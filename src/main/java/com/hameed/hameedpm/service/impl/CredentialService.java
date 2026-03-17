package com.hameed.hameedpm.service.impl;

import com.hameed.hameedpm.exception.ResourceNotFoundException;
import com.hameed.hameedpm.model.Credential;
import com.hameed.hameedpm.service.ICredentialService;
import com.hameed.hameedpm.service.IVaultService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CredentialService implements ICredentialService {

    private final IVaultService vaultService;

    public CredentialService(IVaultService vaultService) {
        this.vaultService = vaultService;
    }

    @Override
    public void addCredential(Credential credential) throws Exception {
        if (credential == null || credential.getServiceName() == null || credential.getServiceName().isEmpty()) {
            throw new IllegalArgumentException("Credential and service name cannot be null or empty");
        }
        if (vaultService.getCurrentVault().getCredentials().isEmpty()) {
            vaultService.getCurrentVault().setCredentials(new ArrayList<>());
        } else if (getCredentialByServiceName(credential.getServiceName()).isPresent()) {
            throw new IllegalArgumentException("Credential for service '" + credential.getServiceName() + "' already exists");
        }

        vaultService.getCurrentVault().getCredentials().add(credential);
        vaultService.persistVault();
    }

    @Override
    public List<Credential> listCredentials() {
        return vaultService.getCurrentVault().getCredentials();
    }

    @Override
    public Optional<Credential> getCredentialByServiceName(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        return vaultService.getCurrentVault().getCredentials().stream()
                .filter(cred -> cred.getServiceName().equalsIgnoreCase(serviceName))
                .findFirst();
    }

    @Override
    public void updateCredential(String serviceName, Credential updatedCredential) throws Exception {
        Credential existingCredential = getCredentialByServiceName(serviceName)
                .orElseThrow(() -> new ResourceNotFoundException("Credential for service '" + serviceName + "' not found"));
        existingCredential.setUsername(updatedCredential.getUsername());
        existingCredential.setPassword(updatedCredential.getPassword());
        existingCredential.setAdditionalInfo(updatedCredential.getAdditionalInfo());
        vaultService.persistVault();
    }

    @Override
    public void deleteCredential(String serviceName) throws Exception {
        vaultService.getCurrentVault().getCredentials().remove(getCredentialByServiceName(serviceName)
                .orElseThrow(() -> new ResourceNotFoundException("Credential for service '" + serviceName + "' not found")));
        vaultService.persistVault();
    }

}
