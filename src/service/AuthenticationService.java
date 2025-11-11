package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.AuthFile;
import model.EncodedAuth;
import model.VaultFile;
import util.ColorUtil;
import util.CryptoUtil;
import util.FileStorageUtil;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import java.security.MessageDigest;

public class AuthenticationService {


    private boolean authenticated = false;
    private String authenticatedUser;
    private final ObjectMapper mapper;


    public AuthenticationService(ObjectMapper mapper) {
        this.mapper = mapper;
        authenticated = false;
        authenticatedUser = null;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(String authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }


    public void login(String username, char[] password) throws Exception {

        // load the auth json and convert to object
        String authJson = FileStorageUtil.loadAuthFile();
        AuthFile authFile = mapper.readValue(authJson, AuthFile.class);

        // get the encoded auth for user
        List<EncodedAuth> encodedAuthList = authFile.authList();
        Optional<EncodedAuth> optionalUserAuthRecord = encodedAuthList.stream().filter(encodedAuth -> encodedAuth.username().equals(username)).findFirst();
        if (optionalUserAuthRecord.isEmpty()) {
            System.out.println("Incorrect username or password.");
            Arrays.fill(password, '\u0000'); // wipe input password
            return;
        }

        // decode base64 fields
        EncodedAuth userAuthRecord = optionalUserAuthRecord.get();
        int iterations = userAuthRecord.iterations();
        byte[] salt = Base64.getDecoder().decode(userAuthRecord.saltBase64());
        byte[] storedHash = Base64.getDecoder().decode(userAuthRecord.hashBase64());

        // use iterations and salt to generate a hash from the entered password
        byte[] generatedHash = CryptoUtil.deriveHash(password, salt, iterations);


        // constant time comparison (prevents timing attacks)
        boolean match = MessageDigest.isEqual(generatedHash, storedHash);

        // wipe hashs
        Arrays.fill(generatedHash, (byte) 0);
        Arrays.fill(storedHash, (byte) 0);

        if (!match) {
            System.out.println(ColorUtil.red("Incorrect username or password. Try again."));
            return;
        }

        System.out.println(ColorUtil.green("Sign-in successful!"));

        // authenticate
        setAuthenticated(true);
        setAuthenticatedUser(username);
    }

    public void signup(String username, char[] password) throws Exception {
        // validate input
        if (!isValidCredentials(username, password)) {
            // throw some exception and find a way to get the validation errors
        };

        // validate username
        if(usernameExists(username)) {
            System.out.println("This username already exists");
            return;
        }


        // create a new user
        createUser(username, password);

        // create a default encrypted vault
        createEncryptedVault(username, password);


        System.out.println("✅ Signup was successful!");

        // authenticate
        setAuthenticatedUser(username);
        setAuthenticated(true);

    }

    public void createEncryptedVault(String username, char[] password) throws Exception {
        int iterations = 100_000;
        byte[] salt = CryptoUtil.generateRandomBytes(16);
        byte[] keyBytes = CryptoUtil.deriveKey(password, salt, iterations);
        SecretKey key = CryptoUtil.keyFromBytes(keyBytes);
        byte[] iv = CryptoUtil.generateRandomBytes(12);
        byte[] signingKey = CryptoUtil.generateRandomBytes(32);

        // start with an empty json object
        String initialJson = """
                {
                  "name": "%s",
                  "signing_key": "%s",
                  "credentials": []
                }
                """.formatted(FileStorageUtil.DEFAULT_VAULT_NAME, Base64.getEncoder().encodeToString(signingKey));

        byte[] ciphertext = CryptoUtil.encrypt(key, initialJson.getBytes(StandardCharsets.UTF_8), iv);
        Arrays.fill(keyBytes, (byte) 0); // wipe memory

        VaultFile vaultFile = new VaultFile(
                iterations,
                new String(Base64.getEncoder().encode(salt)),
                new String(Base64.getEncoder().encode(iv)),
                new String(Base64.getEncoder().encode(ciphertext)),
                CryptoUtil.computeHmac(signingKey, salt, iv, iterations, ciphertext)
        );

        String vaultJson = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(vaultFile);

        // null is passed as a vaultName to use the default
        FileStorageUtil.saveVaultFile(vaultJson, username, FileStorageUtil.DEFAULT_VAULT_NAME);
    }

    public boolean isValidCredentials(String username, char[] password) {
        // TODO: apply some input validations against the password and username
        return true;
    }

    public boolean usernameExists(String username) throws Exception{
        // load auth.json
        String authJson = FileStorageUtil.loadAuthFile();
        AuthFile authFile = mapper.readValue(authJson, AuthFile.class);

        // validate username doesn't already exist
        Optional<EncodedAuth> optionalEncodedAuth = authFile.authList().stream().filter(encodedAuth -> encodedAuth.username().equals(username)).findFirst();
        return optionalEncodedAuth.isPresent();
    }

    private void createUser(String username, char[] password) throws Exception {
        // load auth.json
        String authJson = FileStorageUtil.loadAuthFile();
        AuthFile authFile = mapper.readValue(authJson, AuthFile.class);

        // generate password hash
        int iterations = 100_000;
        byte[] salt = CryptoUtil.generateRandomBytes(16);
        byte[] passwordHash = CryptoUtil.deriveHash(password, salt, iterations);

        // create model.EncodedAuth object and add it to list
        EncodedAuth encodedAuth = new EncodedAuth(
                username,
                iterations,
                new String(Base64.getEncoder().encode(salt)),
                new String(Base64.getEncoder().encode(passwordHash))
        );
        authFile.authList().add(encodedAuth);


        // cleanup passwordHash
        Arrays.fill(passwordHash, (byte) 0);

        // convert back to json and save
        authJson = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(authFile);
        FileStorageUtil.saveAuthFile(authJson);
    }
}
