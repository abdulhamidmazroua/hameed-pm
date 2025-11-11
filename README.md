# Hameed Password Manager

Safely vault your service credentials with a fast, no-nonsense command-line workflow backed by modern crypto defaults.

---

## Why You'll Love It
- Focused CLI that feels at home on any terminal.
- PBKDF2 + AES-GCM encryption with HMAC integrity protection.
- Separate auth layer with salted password hashes.
- Vault files isolated per user under `~/.hameed-pm/`.
- Sensitive keys are wiped from memory as soon as the app can let them go.

---

## Quick Start

### 1. Install prerequisites
- Java 21+ (tested with Amazon Corretto 21).
- The bundled Jackson JARs already live under `lib/`.
- Docker is optional if you prefer containers.

### 2. Run the prebuilt CLI
**Windows (PowerShell)**
```powershell
cd path\to\hameed-password-manager
java -cp "out\artifacts\hameed_password_manager_jar\hameed-password-manager.jar;lib\*" PasswordManager
```

**Linux / macOS**
```bash
cd ~/path/to/hameed-password-manager
java -cp "out/artifacts/hameed_password_manager_jar/hameed-password-manager.jar:lib/*" PasswordManager
```

### 3. Prefer building from source?
**Windows (PowerShell)**
```powershell
cd path\to\hameed-password-manager
javac -cp "lib\*" -d out\production\hameed-password-manager src\**\*.java
java -cp "out\production\hameed-password-manager;lib\*" PasswordManager
```

**Linux / macOS**
```bash
cd ~/path/to/hameed-password-manager
javac -cp "lib/*" -d out/production/hameed-password-manager src/**/*.java
java -cp "out/production/hameed-password-manager:lib/*" PasswordManager
```

### 4. Docker option
```bash
docker build -t hameed-password-manager .
docker run -it --rm -p 5005:5005 hameed-password-manager
```

When the program launches you'll be prompted to `LOGIN` or `SIGNUP`. New users get an empty vault named `safe-vault`.

Vault artifacts live in:
- Windows: `%USERPROFILE%\.hameed-pm\`
- Linux / macOS: `~/.hameed-pm/`

---

## Install as `hameed-pm`

Turn the manager into a first-class command you can call from anywhere.

### Windows (PowerShell script)
1. Create `%USERPROFILE%\bin\hameed-pm.ps1` (or any folder on your `PATH`) with:
   ```powershell
   param([Parameter(ValueFromRemainingArguments = $true)] [string[]] $Args)
   $projectRoot = "C:\path\to\hameed-password-manager"
   java -cp "$projectRoot\out\artifacts\hameed_password_manager_jar\hameed-password-manager.jar;$projectRoot\lib\*" PasswordManager @Args
   ```
2. Ensure the folder is on `PATH`, then restart PowerShell (or run `refreshenv`). Invoke with `hameed-pm`.

### Linux / macOS (shell script)
1. Create `~/bin/hameed-pm` (and `chmod +x` it) containing:
   ```bash
   #!/usr/bin/env bash
   project_root="$HOME/path/to/hameed-password-manager"
   java -cp "$project_root/out/artifacts/hameed_password_manager_jar/hameed-password-manager.jar:$project_root/lib/*" PasswordManager "$@"
   ```
2. Make sure `~/bin` is on `PATH` (update `~/.bashrc`, `~/.zshrc`, etc. if needed).
3. Open a new shell and run `hameed-pm` from anywhere.

Prefer aliases? You can still add `function hameed-pm { ... }` to your shell profile using the same command body.

---

## Command Palette

| Command | Purpose |
|---------|---------|
| `add` | Interactively store a credential with service, username, and password. |
| `list` | Show every service saved in the unlocked vault. |
| `get <service>` | Display the username/password for a service. |
| `update <service> <field> <value>` | Change `service-name`, `username`, or `password`. |
| `delete <service>` | Remove credentials for the service. |
| `help` | See usage guidance with formatting cues. |
| `clear` | Clear the console buffer. |
| `exit` | Securely close the application. |

---

## Security Posture
- Password hashing: `PBKDF2WithHmacSHA256`, 100,000 iterations, random salts.
- Vault encryption: AES-GCM with fresh IV per save; keys derived from PBKDF2 output.
- Integrity: HMAC-SHA256 over salt, IV, iteration count, and ciphertext.
- Memory hygiene: critical byte arrays are zeroed once they're out of scope.

---

## Project Layout
- `src/PasswordManager.java` – CLI entry point and dependency wiring.
- `src/service/` – authentication, vault orchestration, command dispatch.
- `src/util/` – crypto primitives, colorized output, persistence helpers.
- `src/model/` – vault/auth payload records.
- `src/enums/` – command vocabulary & domain enums.
- `lib/` – Jackson annotations/core/databind JARs.
- `out/` – compiled artifacts including the runnable JAR.

---

## Roadmap Ideas
- Harden signup validation (`AuthenticationService#isValidCredentials`).
- Support multiple named vaults per user.
- Store richer metadata (URLs, notes, tags).
- Improve error handling with purpose-built exceptions.
- Enhance UX (tab completion, arrow navigation, or a future GUI).

---

## License
A license hasn’t been published yet. Feel free to suggest one that matches your use case.


