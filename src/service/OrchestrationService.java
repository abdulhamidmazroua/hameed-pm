package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import enums.Command;
import enums.AuthType;
import model.Credential;
import model.Vault;
import model.VaultFile;
import util.CryptoUtil;
import util.FileStorageUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class OrchestrationService {

    private final ObjectMapper mapper;
    private final AuthenticationService authenticationService;
    private final Scanner input;
    private final Console console;

    private Vault vault; // <--- vault stored here while unlocked
    private SecretKey vaultKey;
    private byte[] vaultKeyBytes;


    public OrchestrationService(ObjectMapper mapper, AuthenticationService authenticationService, Scanner input, Console console) {
        this.mapper = mapper;
        this.authenticationService = authenticationService;
        this.input = input;
        this.console = console;
    }

    public void run(String[] args) throws Exception {

        System.out.println("----- Welcome to Hameed Password Manager -----\n");

        try (input) {

            // authenticate
            authenticateUser();

            // open vault returns true or false
            if (!openVault()) {
                System.err.println("You've exceed the number of attempts to open the vault.");
                return;
            }

            System.out.println("Enter a command: ");
            String[] commandLine;

            while (true) {
                System.out.print(">> ");
                commandLine = input.nextLine().trim().split("\\s+");
                Command command;
                try {
                    command = Command.valueOf(commandLine[0].toUpperCase());
                    switch (command) {
                        case ADD -> addNewCredential();
                        case LIST -> listCredentials();
                        case GET -> displayCredential(commandLine[1]);
                        case DELETE -> deleteCredential(commandLine[1]);
                        case HELP -> printUsage();
                        case CLEAR -> clearConsole();
                        case EXIT -> System.exit(0);
                    }
                } catch (IllegalArgumentException ex) {
                    System.out.println("Invalid command.");
                    printUsage();
                }
           }
        } catch (Exception ex) {
            throw ex;
        }
   }

   public void cleanup() {
       if (vaultKeyBytes != null) {
           Arrays.fill(vaultKeyBytes, (byte) 0); // wipe memory
           vaultKeyBytes = null;
       }
       vaultKey = null; // allow GC
       if (vault != null && vault.getCredentials() != null)
           vault.getCredentials().clear();

       authenticationService.setAuthenticated(false);
       authenticationService.setAuthenticatedUser(null);
   }

    private void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("Could not clear console.");
        }
    }


    private void authenticateUser() throws Exception {
       while (!authenticationService.isAuthenticated()) {
           System.out.print("Choose login/signup: ");
           AuthType authType;
           try {
               authType = AuthType.valueOf(input.nextLine().toUpperCase());
               inputAuthentication(authType);
           } catch (IllegalArgumentException ex) {
               System.out.println("Invalid option. Try again");
           }
       }
   }

   private void inputAuthentication(AuthType option) throws Exception {
        String username;
        char[] password;
        int attempts = 0;
        if (option == AuthType.LOGIN) System.out.println("Please Enter your credentials ...");
        else if (option == AuthType.SIGNUP) System.out.println("Creating a new user, please enter the following fields ...");
        else System.out.println("Invalid option. Try again.");
        do {
            // username
            System.out.print("username: ");
            username = readUsernameFromConsoleOrFallback();

            // password
            System.out.print("password: ");
            password = readPasswordFromConsoleOrFallback();

            // password again (only for sign-up)
            if (option == AuthType.SIGNUP) {
                System.out.print("password again: ");
                char[] passwordAgain = readPasswordFromConsoleOrFallback();
                if (!Arrays.equals(password, passwordAgain)) {
                    System.err.println("Passwords do not match.");
                    return;
                }
            }
            System.out.println();

            if (option == AuthType.LOGIN) authenticationService.login(username, password);
            else if (option == AuthType.SIGNUP) authenticationService.signup(username, password);
            else System.out.println("Invalid option. Try again.");
            Arrays.fill(password, '\u0000'); // wipe input password
            attempts++;
        } while (!authenticationService.isAuthenticated() && attempts < 3);
   }

   private String readUsernameFromConsoleOrFallback() {
        return (console != null) ? console.readLine() : input.nextLine();
   }

   private char[] readPasswordFromConsoleOrFallback() {
        return (console != null) ? console.readPassword() : input.nextLine().toCharArray();
   }

   private boolean openVault()  throws Exception {

       System.out.println("Which Vault do you want to open, please use the following type: <VaultName>");

       // use the file storage to retrieve all vaults
       List<String> userVaultNames = FileStorageUtil.getUserVaultNames(authenticationService.getAuthenticatedUser());

       // display them for the user to choose from
       for (int i = 0; i < userVaultNames.size(); i++) {
           System.out.println("\u001B[31m" + (i + 1) + ". \u001B[0m " + userVaultNames.get(i));
       }
       String vaultChosen;
       while (true) {
           System.out.print(">> ");
           vaultChosen = input.nextLine();
           if (!userVaultNames.contains(vaultChosen)) {
               System.err.println("There is no vault with this name: " + vaultChosen);
           } else {
               break;
           }
       }

       // load and convert from json to object
       VaultFile vaultFile = mapper.readValue(
               FileStorageUtil.loadVaultFile(authenticationService.getAuthenticatedUser(), vaultChosen),
               VaultFile.class);


       // get and decode the values
       int iterations = vaultFile.getIterations();
       byte[] salt = Base64.getDecoder().decode(vaultFile.getSaltBase64());
       byte[] iv = Base64.getDecoder().decode(vaultFile.getIvBase64());
       byte[] ciphertext = Base64.getDecoder().decode(vaultFile.getCiphertextBase64());

       // enter the password again to open vault
       char[] password;
       boolean isValidKey = false;
       int attempts = 0;
       do {
           // enter password again for vault
           System.out.print("Enter password again to unlock vault: ");
           password = readPasswordFromConsoleOrFallback();

           // derive the key
           vaultKeyBytes = CryptoUtil.deriveKey(password, salt, iterations);
           vaultKey = CryptoUtil.keyFromBytes(vaultKeyBytes);
           Arrays.fill(password, '\u0000'); // wipe password

           // decrypt attempt
           try {
               String decryptedVault = new String(CryptoUtil.decrypt(vaultKey, ciphertext, iv), StandardCharsets.UTF_8);

               // get the hash and check tampering
               Vault tempVault = mapper.readValue(decryptedVault, Vault.class);
               integrityCheck(vaultFile.getHashBase64(), tempVault, salt, iv, iterations, ciphertext);


               // write to the current vault field
               this.vault = tempVault;
               isValidKey = true;
               System.out.println();
               System.out.println("✅ Vault opening was successful!");
               System.out.println("use the following commands: ");
               printUsage();

           } catch (BadPaddingException ex) {
               System.out.println("Invalid Password.");
               attempts++;
           } catch (SecurityException ex) {
               System.err.println(ex.getMessage());
               throw ex;
           } catch (Exception ex) {
               throw ex;
           }
       } while (!isValidKey && attempts < 3);

       return isValidKey;

}

   private void addNewCredential() throws Exception {
       System.out.print("Service Name: ");
       String serviceName = input.nextLine();
       System.out.print("username: ");
       String username = input.nextLine();
       System.out.print("password: ");
       String password = input.nextLine();

       // create credential
       Credential credential = new Credential();
       credential.setServiceName(serviceName);
       credential.setUsername(username);
       credential.setPassword(password);

       // add and save
       vault.add(credential);
       saveVault();

       System.out.println("Credential added.");
   }

   private void listCredentials() {
       for (int i = 0; i < vault.getCredentials().size(); i++) {
           System.out.println("\u001B[31m " + (i+1) + ". \u001B[0m " + vault.getCredentials().get(i).getServiceName());
       }
   }

   private void displayCredential(String serviceName) {
        // Find the encrypted credentials in the file and decrypt it
        Optional<Credential> optionalCredential = vault.find(serviceName);
        Credential credential;
        if (optionalCredential.isPresent()) {
            credential = optionalCredential.get();

            System.out.println("service name: " + credential.getServiceName());
            System.out.println("username: " + credential.getUsername());
            System.out.println("password: " + credential.getPassword());
        } else {
            System.out.println("Could not find any credentials in this vault with this name: " + serviceName);
        }

   }


   private void deleteCredential(String serviceName) throws Exception {

       if (vault.remove(serviceName)) {
           saveVault();
           System.out.println("Removed successfully");

       }
       else System.out.println("Credential for this service was not found");

   }

   private void printUsage() {
       System.out.println("Usage - 1: add");
       System.out.println("Usage - 2: list");
       System.out.println("Usage - 3: get <service-name>");
       System.out.println("Usage - 4: delete <service-name>");
       System.out.println("Usage - 5: help");
       System.out.println("Usage - 6: clear");
       System.out.println("Usage - 7: exit");
       System.out.println();
   }

   private void integrityCheck(String storedHashBase64, Vault decryptedVault, byte[] salt, byte[] iv, int iterations, byte[] ciphertext) throws SecurityException, NoSuchAlgorithmException, InvalidKeyException {
       String generatedHash = CryptoUtil.computeHmac(
               Base64.getDecoder().decode(decryptedVault.getSigningKey()),
               salt,
               iv,
               iterations,
               ciphertext);

       if (!MessageDigest.isEqual(Base64.getDecoder().decode(storedHashBase64), Base64.getDecoder().decode(generatedHash))) {
           throw new SecurityException("⚠️ Vault tampered with!");
       }
   }
   private void saveVault() throws Exception {

       // load the vault file (includes metadata and ciphertext of vault)
       String vaultFileJson = FileStorageUtil.loadVaultFile(authenticationService.getAuthenticatedUser(), vault.getName());
       VaultFile vaultFile = mapper.readValue(vaultFileJson, VaultFile.class);

       // encrypt the new vault with a new iv
       byte[] newIV = CryptoUtil.generateRandomBytes(12);
       String updatedVaultJson = mapper.writerWithDefaultPrettyPrinter()
               .writeValueAsString(vault);
       byte[] updatedEncryptedJson = CryptoUtil.encrypt(vaultKey, updatedVaultJson.getBytes(), newIV);

       // replace vault file fields with the new iv and the new ciphertext
       vaultFile.setIvBase64(new String(Base64.getEncoder().encode(newIV), StandardCharsets.UTF_8));
       vaultFile.setCiphertextBase64(new String(Base64.getEncoder().encode(updatedEncryptedJson), StandardCharsets.UTF_8));

       // add the new hash to avoid tampering
       vaultFile.setHashBase64(CryptoUtil.computeHmac(
               Base64.getDecoder().decode(vault.getSigningKey()),
               Base64.getDecoder().decode(vaultFile.getSaltBase64()),
               Base64.getDecoder().decode(vaultFile.getIvBase64()),
               vaultFile.getIterations(),
               updatedEncryptedJson));

       // save the updated vault file
       String vaultJson = mapper.writerWithDefaultPrettyPrinter()
               .writeValueAsString(vaultFile);
       FileStorageUtil.saveVaultFile(vaultJson, authenticationService.getAuthenticatedUser(), vault.getName());

   }

}
