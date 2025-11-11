package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileStorageUtil {

    private static final String HOME_PATH = System.getProperty("user.home");
    private static final String MAIN_DIR = ".hameed-pm";
    private static final String AUTH_DIR = ".auth";
    private static final String AUTH_FILENAME = "auth";
    private static final String AUTH_FILE_EXTENSION = ".json";

    private static final String VAULT_EXTENSION = ".json";

    public static final String DEFAULT_VAULT_NAME = "safe-vault";

    public static Path getVaultPath(String username, String vaultName) throws SecurityException {

        // validate input for security
        StringUtil.requireSafeName(username, "username");
        StringUtil.requireSafeName(vaultName, "vault-name");

        Path baseDir = Paths.get(HOME_PATH, MAIN_DIR);
        Path target = baseDir.resolve(username).resolve(vaultName + VAULT_EXTENSION).normalize();
        if (!target.startsWith(baseDir)) {
            throw new SecurityException("Invalid path after normalization");
        }

        return target;

    }

    public static List<String> getUserVaultNames(String username) throws IOException, SecurityException {

        // validate input for security
        StringUtil.requireSafeName(username, "username");

        Path baseDir = Paths.get(HOME_PATH, MAIN_DIR);
        Path target = baseDir.resolve(username).normalize();
        if (!target.startsWith(baseDir)) {
            throw new SecurityException("Invalid path after normalization");
        }

        return Files.list(target)
                .map(p -> p.getFileName().toString())
                .map(p -> p.replace(VAULT_EXTENSION, ""))
                .toList();
    }

    public static String loadVaultFile(String username, String vaultName) throws Exception {
        Path vaultPath = getVaultPath(username, vaultName);
        return Files.readString(vaultPath);
    }
    
    public static void saveVaultFile(String vaultContent, String username, String vaultName) throws Exception {
        Path vaultPath = getVaultPath(username, vaultName);

        // ensure parent directories exist
        if (!Files.exists(vaultPath.getParent())) {
            Files.createDirectories(vaultPath.getParent());
        }

        Files.writeString(vaultPath, vaultContent);
    }

    public static Path getAuthFilePath() throws SecurityException {
        Path baseDir = Paths.get(HOME_PATH, MAIN_DIR);
        Path target = baseDir.resolve(AUTH_DIR).resolve(AUTH_FILENAME + AUTH_FILE_EXTENSION).normalize();
        if (!target.startsWith(baseDir)) {
            throw new SecurityException("Invalid path after normalization");
        }

        return target;
    }

    public static String loadAuthFile() throws Exception {
        Path authFilePath = getAuthFilePath();
        return Files.exists(authFilePath) ? Files.readString(authFilePath) : "{ \"auth_list\": []}";
    }

    public static void saveAuthFile(String authContent) throws Exception {
        Path authFilePath = getAuthFilePath();

        // ensure parent directories exist
        if (!Files.exists(authFilePath.getParent())) {
            Files.createDirectories(authFilePath.getParent());
        }

        Files.writeString(authFilePath, authContent);
    }

}
