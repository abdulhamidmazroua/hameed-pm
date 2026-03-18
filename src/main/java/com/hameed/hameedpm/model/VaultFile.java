package com.hameed.hameedpm.model;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VaultFile {
    private int iterations;
    private byte[] salt;
    private byte[] iv;
    private byte[] ciphertext;
    private byte[] hash;

    public VaultFile() {}

    public VaultFile(int iterations, byte[] salt, byte[] iv, byte[] ciphertext, byte[] hash) {
        this.iterations = iterations;
        this.salt = salt;
        this.iv = iv;
        this.ciphertext = ciphertext;
        this.hash = hash;
    }

    public int getIterations() { return iterations; }
    public void setIterations(int iterations) { this.iterations = iterations; }

    public byte[] getSalt() { return salt; }
    public void setSalt(byte[] salt) { this.salt = salt; }

    public byte[] getIv() { return iv; }
    public void setIv(byte[] iv) { this.iv = iv; }

    public byte[] getCiphertext() { return ciphertext; }
    public void setCiphertext(byte[] ciphertext) { this.ciphertext = ciphertext; }

    public byte[] getHash() { return hash; }
    public void setHash(byte[] hash) { this.hash = hash; }
}
