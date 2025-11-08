package util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileStorageUtil {

    private static final String HOME_PATH = System.getProperty("user.home") + "/";
    private static final String MAIN_DIR = ".hameed-pm/";
    private static final String VAULT_EXTENSION = ".json";
    private static final String AUTH_DIR = ".auth/";
    private static final String AUTH_FILENAME = "auth.json";

    public static final String DEFAULT_VAULT_NAME = "safe-vault";

    public static String getVaultPath(String username, String vaultName) {
        String vaultPath = username + "/" +
                ((vaultName != null) ? vaultName : DEFAULT_VAULT_NAME);
        return HOME_PATH + MAIN_DIR + vaultPath + VAULT_EXTENSION;
    }

    public static List<String> getUserVaultNames(String username) throws IOException {
        Path path = Path.of(HOME_PATH + MAIN_DIR + username + "/");
        return Files.list(path)
                .map(p -> p.getFileName().toString())
                .map(p -> p.replace(VAULT_EXTENSION, ""))
                .toList();
    }

    public static String loadVaultFile(String username, String vaultName) throws Exception {
        String vaultPath = FileStorageUtil.getVaultPath(username, vaultName);
        return Files.readString(Path.of(vaultPath));
    }
    
    public static void saveVaultFile(String vaultContent, String username, String vaultName) throws Exception {
        String vaultPathString = FileStorageUtil.getVaultPath(username, vaultName);
        Path vaultPath = Path.of(vaultPathString);

        // ensure parent directories exist
        if (!Files.exists(vaultPath.getParent())) {
            Files.createDirectories(vaultPath.getParent());
        }

        Files.writeString(vaultPath, vaultContent);
    }

    public static String loadAuthFile() throws Exception {
        Path path = Path.of(HOME_PATH + MAIN_DIR + AUTH_DIR + AUTH_FILENAME);
        return Files.exists(path) ? Files.readString(path) : "{ \"auth_list\": []}";
    }

    public static void saveAuthFile(String authContent) throws Exception {
        Path path = Path.of(HOME_PATH + MAIN_DIR + AUTH_DIR + AUTH_FILENAME);

        // ensure parent directories exist
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        Files.writeString(path, authContent);
    }

}
