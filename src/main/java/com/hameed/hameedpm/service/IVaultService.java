package com.hameed.hameedpm.service;


import com.hameed.hameedpm.model.Vault;


public interface IVaultService {
    void persistVault() throws Exception;
    Vault getCurrentVault();
    boolean isVaultUnlocked();
    boolean unlockVault(String vaultName, char[] password);
    boolean vaultExists();
    void createEncryptedVault(String vaultName, char[] password) throws Exception;
}
