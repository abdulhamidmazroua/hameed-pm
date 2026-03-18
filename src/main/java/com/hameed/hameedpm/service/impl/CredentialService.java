package com.hameed.hameedpm.service.impl;

import com.hameed.hameedpm.exception.ResourceNotFoundException;
import com.hameed.hameedpm.model.Credential;
import com.hameed.hameedpm.service.ICredentialService;
import com.hameed.hameedpm.service.IVaultService;
import com.hameed.hameedpm.util.StringUtil;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class CredentialService implements ICredentialService {

    private final IVaultService vaultService;

    public CredentialService(IVaultService vaultService) {
        this.vaultService = vaultService;
    }

    @Override
    public void saveCredential(Credential credential) throws Exception {
        addCredential(credential);
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

    @Override
    public void addAll(List<Credential> credentials, CommandContext ctx) throws Exception {
        credentials.forEach(credential -> {
            try{
                addCredential(credential);
            } catch (IllegalArgumentException ex) {
                ctx.outputWriter().println(ex.getMessage());
                ctx.outputWriter().flush();
            }
        });

        // after all viable credentials added to the vault
        // persist
        vaultService.persistVault();
    }

    private void addCredential(Credential credential) throws IllegalArgumentException {
        if (credential == null || credential.getServiceName() == null || credential.getServiceName().isEmpty()) {
            throw new IllegalArgumentException("Credential and service name cannot be null or empty");
        }
        if (vaultService.getCurrentVault().getCredentials().isEmpty()) {
            vaultService.getCurrentVault().setCredentials(new ArrayList<>());
        } else if (getCredentialByServiceName(credential.getServiceName()).isPresent()) {
            throw new IllegalArgumentException("Credential for service '" + credential.getServiceName() + "' already exists");
        }

        vaultService.getCurrentVault().getCredentials().add(credential);
    }
}
