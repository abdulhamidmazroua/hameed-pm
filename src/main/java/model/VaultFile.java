package model;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VaultFile {

    private int iterations;
    private String saltBase64;
    private String ivBase64;
    private String ciphertextBase64;
    private String hashBase64;

    public VaultFile() {};

    public VaultFile(int iterations, String saltBase64, String ivBase64, String ciphertextBase64, String hashBase64) {
        this.iterations = iterations;
        this.saltBase64 = saltBase64;
        this.ivBase64 = ivBase64;
        this.ciphertextBase64 = ciphertextBase64;
        this.hashBase64 = hashBase64;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public String getSaltBase64() {
        return saltBase64;
    }

    public void setSaltBase64(String saltBase64) {
        this.saltBase64 = saltBase64;
    }

    public String getIvBase64() {
        return ivBase64;
    }

    public void setIvBase64(String ivBase64) {
        this.ivBase64 = ivBase64;
    }

    public String getCiphertextBase64() {
        return ciphertextBase64;
    }

    public void setCiphertextBase64(String ciphertextBase64) {
        this.ciphertextBase64 = ciphertextBase64;
    }

    public String getHashBase64() {
        return hashBase64;
    }

    public void setHashBase64(String hashBase64) {
        this.hashBase64 = hashBase64;
    }
}
