package com.hameed.hameedpm;

import com.hameed.hameedpm.service.IVaultService;
import com.hameed.hameedpm.util.FileStorageUtil;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Order(1) // Ensure this runs before any other ApplicationRunner
public class VaultUnlockRunner implements CommandLineRunner {

    private final LineReader lineReader;  // inject Spring Shell's LineReader
    private final IVaultService vaultSecurityService;

    public VaultUnlockRunner(LineReader lineReader, IVaultService vaultSecurityService) {
        this.lineReader = lineReader;
        this.vaultSecurityService = vaultSecurityService;
    }

    @Override
    public void run(String... args) throws Exception {

        Terminal terminal = lineReader.getTerminal(); // get terminal from the LineReader

        if (!vaultSecurityService.vaultExists()) {
            terminal.writer().println("No vault found. Let's create one.");
            terminal.writer().println("Your master password will be used to encrypt and protect your vault.");
            printCriteria(terminal);
            terminal.writer().flush();

            char[] password = promptPassword("Set master password: ");
            char[] confirm  = promptPassword("Confirm master password: ");

            if (!validPassword(password)) {
                terminal.writer().println("Password does not meet strength requirements.");
                printCriteria(terminal);
                terminal.writer().flush();
                Arrays.fill(password, '\0');
                Arrays.fill(confirm,  '\0');
                System.exit(1);
            }

            if (!Arrays.equals(password, confirm)) {
                terminal.writer().println("Passwords do not match. Exiting.");
                terminal.writer().flush();
                Arrays.fill(password, '\0');
                Arrays.fill(confirm,  '\0');
                System.exit(1);
            }

            vaultSecurityService.createEncryptedVault(FileStorageUtil.DEFAULT_VAULT_NAME, password);
            Arrays.fill(password, '\0');
            Arrays.fill(confirm,  '\0');

            terminal.writer().println("Vault created successfully.");
            terminal.writer().flush();
            return;
        }

        // Vault exists — prompt to unlock, allow 3 attempts
        for (int attempt = 1; attempt <= 3; attempt++) {
            char[] password = promptPassword("Master password: ");

            if (vaultSecurityService.unlockVault(FileStorageUtil.DEFAULT_VAULT_NAME, password)) {
                Arrays.fill(password, '\0');
                terminal.writer().println("Vault unlocked. Welcome.");
                terminal.writer().flush();

                return;
            }

            Arrays.fill(password, '\0');
            int remaining = 3 - attempt;
            if (remaining > 0) {
                terminal.writer().println("Wrong password. Attempts remaining: " + remaining);
            } else {
                terminal.writer().println("Too many failed attempts. Exiting.");
            }
            terminal.writer().flush();
        }

        System.exit(1);
    }

    private char[] promptPassword(String prompt) {
        String input = lineReader.readLine(prompt, '*');
        return input != null ? input.toCharArray() : new char[0];
    }

    private boolean validPassword(char[] password) {
        if (password.length < 12) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password) {
            if (Character.isUpperCase(c))      hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c))     hasDigit = true;
            else if ("!@#$%^&*()_+-=[]{}|;':\",.<>/?".indexOf(c) >= 0) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private void printCriteria(Terminal terminal) {
        terminal.writer().println("Password must be at least 12 characters long and include:");
        terminal.writer().println("- At least one uppercase letter");
        terminal.writer().println("- At least one lowercase letter");
        terminal.writer().println("- At least one digit");
        terminal.writer().println("- At least one special character (!@#$%^&* etc.)");
    }
}