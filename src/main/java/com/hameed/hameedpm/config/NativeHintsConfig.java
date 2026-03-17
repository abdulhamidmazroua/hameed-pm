package com.hameed.hameedpm.config;

import com.hameed.hameedpm.model.Credential;
import com.hameed.hameedpm.model.Vault;
import com.hameed.hameedpm.model.VaultFile;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(NativeHintsConfig.AppRuntimeHints.class)
public class NativeHintsConfig {

    static class AppRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection()
                    .registerType(VaultFile.class,  MemberCategory.values())
                    .registerType(Vault.class,       MemberCategory.values())
                    .registerType(Credential.class,  MemberCategory.values());
        }
    }
}
