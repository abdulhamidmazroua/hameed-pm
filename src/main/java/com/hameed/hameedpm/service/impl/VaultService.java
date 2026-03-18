package com.hameed.hameedpm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hameed.hameedpm.model.Vault;
import com.hameed.hameedpm.model.VaultFile;
import com.hameed.hameedpm.service.IVaultService;
import com.hameed.hameedpm.util.CryptoUtil;
import com.hameed.hameedpm.util.VaultFileUtil;
import com.hameed.hameedpm.util.StringUtil;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;


@Service
public class VaultService implements IVaultService {

    private boolean vaultUnlocked;
    private final ObjectMapper mapper;
    private SecretKey vaultKey;
    private Vault vault;

    private static final int IV_LENGTH        = 12;
    private static final int SIGNING_KEY_LENGTH = 32;
    private static final int ITERATIONS       = 200_000;
    private static final int SALT_LENGTH      = 16;

    public VaultService() {
        this.mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
    }

    @Override
    public void persistVault() throws Exception {
        String vaultFileJson = VaultFileUtil.loadVaultFile(vault.getName());
        VaultFile vaultFile  = mapper.readValue(vaultFileJson, VaultFile.class);

        byte[] newIv      = CryptoUtil.generateRandomBytes(IV_LENGTH);
        byte[] plaintext  = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(vault)
                .getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = CryptoUtil.encrypt(vaultKey, plaintext, newIv);
        byte[] hash       = CryptoUtil.computeHmac(
                vault.getSigningKey(),
                vaultFile.getSalt(),      // already byte[]
                newIv,
                vaultFile.getIterations(),
                ciphertext);

        vaultFile.setIv(newIv);
        vaultFile.setCiphertext(ciphertext);
        vaultFile.setHash(hash);

        VaultFileUtil.saveVaultFile(
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(vaultFile),
                vault.getName());
    }


    public Vault getCurrentVault() {
        if (!vaultUnlocked) {
            throw new IllegalStateException("Vault is locked");
        }
        return vault;
    }

    @Override
    public boolean isVaultUnlocked() {
        return vaultUnlocked;
    }

    @Override
    public boolean unlockVault(String vaultName, char[] password) {
        try {
            StringUtil.requireSafeName(vaultName, "vaultName");

            VaultFile vaultFile = mapper.readValue(
                    VaultFileUtil.loadVaultFile(vaultName), VaultFile.class);

            // all fields are already byte[] — no decoding needed
            int    iterations = vaultFile.getIterations();
            byte[] salt       = vaultFile.getSalt();
            byte[] iv         = vaultFile.getIv();
            byte[] ciphertext = vaultFile.getCiphertext();
            byte[] storedHash = vaultFile.getHash();

            byte[]    keyBytes = CryptoUtil.deriveKey(password, salt, iterations);
            SecretKey key      = CryptoUtil.keyFromBytes(keyBytes);
            wipe(keyBytes);

            byte[] plaintext     = CryptoUtil.decrypt(key, ciphertext, iv);
            Vault  decryptedVault = mapper.readValue(plaintext, Vault.class);

            integrityCheck(storedHash, decryptedVault.getSigningKey(), salt, iv, iterations, ciphertext);

            this.vault         = decryptedVault;
            this.vaultKey      = key;
            this.vaultUnlocked = true;
            return true;

        } catch (BadPaddingException | SecurityException ex) {
            return false;
        } catch (Exception ex) {
            System.out.println("Unexpected error during vault unlock: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean vaultExists() {
        return VaultFileUtil.vaultFileExists(VaultFileUtil.DEFAULT_VAULT_NAME);
    }

    @Override
    public void createEncryptedVault(String vaultName, char[] password) throws Exception {
        byte[] iv         = null;
        byte[] signingKey = null;
        byte[] salt       = null;
        byte[] keyBytes   = null;

        try {
            StringUtil.requireSafeName(vaultName, "vaultName");
            StringUtil.requireValidPassword(password);

            iv         = CryptoUtil.generateRandomBytes(IV_LENGTH);
            signingKey = CryptoUtil.generateRandomBytes(SIGNING_KEY_LENGTH);
            salt       = CryptoUtil.generateRandomBytes(SALT_LENGTH);
            keyBytes   = CryptoUtil.deriveKey(password, salt, ITERATIONS);

            SecretKey key = CryptoUtil.keyFromBytes(keyBytes);

            Vault initialVault = new Vault();
            initialVault.setName(vaultName);
            initialVault.setSigningKey(Arrays.copyOf(signingKey, signingKey.length));
            initialVault.setCredentials(new ArrayList<>());

            byte[] plaintext  = mapper.writeValueAsBytes(initialVault);
            byte[] ciphertext = CryptoUtil.encrypt(key, plaintext, iv);
            byte[] hash       = CryptoUtil.computeHmac(signingKey, salt, iv, ITERATIONS, ciphertext);

            // all byte[] — Jackson encodes to Base64 in JSON automatically
            VaultFile vaultFile = new VaultFile(ITERATIONS, salt, iv, ciphertext, hash);

            VaultFileUtil.saveVaultFile(
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(vaultFile),
                    vaultName);

            this.vaultKey      = key;
            this.vault         = initialVault;
            this.vaultUnlocked = true;

        } catch (Exception e) {
            throw new Exception("Failed to create vault: " + e.getMessage(), e);
        } finally {
            wipe(keyBytes);
            wipe(salt);
            wipe(iv);
            wipe(signingKey);
            if (password != null) Arrays.fill(password, '\0');
        }
    }

    private void integrityCheck(byte[] storedHash, byte[] signingKey,
                                byte[] salt, byte[] iv,
                                int iterations, byte[] ciphertext)
            throws SecurityException, NoSuchAlgorithmException, InvalidKeyException {

        byte[] generatedHash = CryptoUtil.computeHmac(
                signingKey, salt, iv, iterations, ciphertext);

        if (!MessageDigest.isEqual(storedHash, generatedHash)) {
            throw new SecurityException("Vault integrity check failed — possible tampering detected.");
        }
    }

    private void wipe(byte[] bytes) {
        if (bytes != null) Arrays.fill(bytes, (byte) 0);
    }

}
