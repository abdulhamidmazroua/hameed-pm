package com.hameed.hameedpm.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

public class FileStorageUtil {

    private static final String HOME_PATH = System.getProperty("user.home");
    private static final String MAIN_DIR = ".hameed-pm";
    private static final String VAULT_EXTENSION = ".json";
    public static final String DEFAULT_VAULT_NAME = "safe-vault";

    public static Path getVaultPath(String vaultName) throws SecurityException {

        StringUtil.requireSafeName(vaultName, "vaultName");

        Path baseDir = Paths.get(HOME_PATH, MAIN_DIR);
        Path target = baseDir.resolve(vaultName + VAULT_EXTENSION).normalize();
        if (!target.startsWith(baseDir)) {
            throw new SecurityException("Invalid path after normalization");
        }
        return target;
    }

    public static boolean vaultFileExists(String vaultName) {

        try {
            Path vaultPath = getVaultPath(vaultName);
            return Files.exists(vaultPath);
        } catch (SecurityException e) {
            return false;
        }
    }

    public static String loadVaultFile(String vaultName) throws Exception {
        Path vaultPath = getVaultPath(vaultName);
        return Files.readString(vaultPath);
    }

    public static void saveVaultFile(String vaultContent, String vaultName) throws Exception {
        Path vaultPath = getVaultPath(vaultName);

        // ensure parent directories exist
        if (!Files.exists(vaultPath.getParent())) {
            Files.createDirectories(vaultPath.getParent());
        }

        Files.writeString(vaultPath, vaultContent);
    }

}
