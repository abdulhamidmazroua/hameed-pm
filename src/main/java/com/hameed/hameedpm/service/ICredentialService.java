package com.hameed.hameedpm.service;

import com.hameed.hameedpm.model.Credential;
import org.springframework.shell.core.command.CommandContext;

import java.util.List;
import java.util.Optional;

public interface ICredentialService {
    void saveCredential(Credential credential) throws Exception;
    List<Credential> listCredentials();
    Optional<Credential> getCredentialByServiceName(String serviceName);
    void updateCredential(String serviceName, Credential updatedCredential) throws Exception;
    void deleteCredential(String serviceName) throws Exception;
    void addAll(List<Credential> credentials, CommandContext ctx) throws Exception;

}
