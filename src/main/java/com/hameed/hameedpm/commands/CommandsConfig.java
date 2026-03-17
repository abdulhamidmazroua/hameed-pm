package com.hameed.hameedpm.commands;

import com.hameed.hameedpm.exception.ResourceNotFoundException;
import com.hameed.hameedpm.model.Credential;
import com.hameed.hameedpm.service.ICredentialService;
import com.hameed.hameedpm.service.IVaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.*;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.availability.AvailabilityProvider;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Configuration
public class CommandsConfig {

    private final IVaultService authenticationService;
    private final ICredentialService credentialService;
    private final ComponentFlow.Builder componentFlowBuilder;

    @Autowired
    public CommandsConfig(IVaultService authenticationService, ICredentialService credentialService, ComponentFlow.Builder componentFlowBuilder) {
        this.authenticationService = authenticationService;
        this.credentialService = credentialService;
        this.componentFlowBuilder = componentFlowBuilder;
    }

    // helper to reduce boilerplate on every command
    private String requireServiceName(CommandContext ctx) {
        CommandArgument arg = ctx.getArgumentByIndex(0);
        if (arg == null || arg.value() == null || arg.value().isBlank()) {
            throw new IllegalArgumentException("Service name is required. Usage: <command> <service-name>");
        }
        return arg.value();
    }

    @Bean
    public Command addCredentialsCommand() {
        return Command.builder()
                .name("add")
                .description("Add a new credential")
                .help("Adds a new credential. Usage: add <service-name>")
                // no .options() — service name is now a positional argument
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String serviceName = requireServiceName(ctx);

                    ComponentFlow.ComponentFlowResult basicResult = componentFlowBuilder.clone()
                            .reset()
                            .withStringInput("username")
                            .name("Username / Email:")
                            .required()
                            .and()
                            .withStringInput("password")
                            .name("Password:")
                            .maskCharacter('*')
                            .required()
                            .and()
                            .withConfirmationInput("hasMoreInfo")
                            .name("Do you want to add extra info?")
                            .and()
                            .build()
                            .run();

                    String username   = basicResult.getContext().get("username",    String.class);
                    String password   = basicResult.getContext().get("password",    String.class);
                    boolean hasMoreInfo = basicResult.getContext().get("hasMoreInfo", Boolean.class);

                    Map<String, String> additionalInfo = new LinkedHashMap<>();
                    while (hasMoreInfo) {
                        ComponentFlow.ComponentFlowResult extraResult = componentFlowBuilder.clone()
                                .reset()
                                .withStringInput("key")
                                .name("Key")
                                .required()
                                .and()
                                .withStringInput("value")
                                .name("Value")
                                .required()
                                .and()
                                .withConfirmationInput("addAnother")
                                .name("Add another entry?")
                                .and()
                                .build()
                                .run();

                        String key   = extraResult.getContext().get("key",   String.class);
                        String value = extraResult.getContext().get("value", String.class);
                        additionalInfo.put(key, value);
                        hasMoreInfo = extraResult.getContext().get("addAnother", Boolean.class);
                    }

                    try {
                        credentialService.addCredential(new Credential(serviceName, username, password, additionalInfo));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    ctx.outputWriter().println("Credential for '" + serviceName + "' saved successfully.");
                });
    }

    @Bean
    public Command listCredentialsCommand() {
        return Command.builder()
                .name("list")
                .description("List credentials")
                .help("A command that lists all credentials in the vault. Usage: list [-d | --detailed]")
                .options(CommandOption.with()
                        .shortName('d')
                        .longName("detailed")
                        .required(false)
                        .type(boolean.class)
                        .build())
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    CommandOption detailedOption = ctx.getOptionByShortName('d');
                    boolean detailed = detailedOption != null &&
                            detailedOption.value() != null &&
                            Boolean.parseBoolean(detailedOption.value());
                    if (detailed) {
                        credentialService.listCredentials().forEach(cred -> printCredential(ctx, cred));
                    } else {
                        List<Credential> credentials = credentialService.listCredentials();
                        for (int i = 0; i < credentials.size(); i++) {
                            ctx.outputWriter().println((i + 1) + ". " + credentials.get(i).getServiceName());
                        }
                    }
                });
    }

    @Bean
    public Command getCredentialCommand() {
        return Command.builder()
                .name("get")
                .description("Get specific credential details")
                .help("A command that displays the credential details. Usage: get <service-name>")
                // no .options()
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String serviceName = requireServiceName(ctx);
                    Credential cred = credentialService.getCredentialByServiceName(serviceName)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Credential with this service name was not found: " + serviceName));
                    printCredential(ctx, cred);
                });
    }

    @Bean
    public Command updateCredentialCommand() {
        return Command.builder()
                .name("update")
                .description("Update specific credential details")
                .help("A command to update credential details. Usage: update <service-name>")
                // no .options()
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String serviceName = requireServiceName(ctx);

                    Credential cred = credentialService.getCredentialByServiceName(serviceName)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Credential with this service name was not found: " + serviceName));

                    Credential updatedCred = new Credential(cred);
                    printCredential(ctx, cred);
                    boolean keepUpdating = true;

                    while (keepUpdating) {
                        ComponentFlow.ComponentFlowResult updateFieldSelectorResult = componentFlowBuilder.clone()
                                .reset()
                                .withSingleItemSelector("updateFieldSelector")
                                .name("Choose what you want to update:")
                                .selectItems(List.of(
                                        SelectItem.of("Username / Email", "username"),
                                        SelectItem.of("Password",         "password"),
                                        SelectItem.of("Additional Info",  "additionalInfo"),
                                        SelectItem.of("Done",             "exit")
                                ))
                                .and()
                                .build()
                                .run();

                        String fieldToUpdate = updateFieldSelectorResult.getContext().get("updateFieldSelector", String.class);

                        switch (fieldToUpdate) {
                            case "username" -> {
                                ComponentFlow.ComponentFlowResult userUpdateResult = componentFlowBuilder.clone().reset()
                                        .withStringInput("username")
                                        .name("Updated Username / Email: ")
                                        .defaultValue("")
                                        .required()
                                        .and()
                                        .build().run();

                                String updatedUsername = userUpdateResult.getContext().get("username", String.class);
                                if (updatedUsername == null || updatedUsername.isBlank()) {
                                    ctx.outputWriter().println("Error: the updated username cannot be empty.");
                                } else {
                                    updatedCred.setUsername(updatedUsername);
                                }
                            }
                            case "password" -> {
                                ComponentFlow.ComponentFlowResult passwordUpdateResult = componentFlowBuilder.clone().reset()
                                        .withStringInput("password")
                                        .name("Updated Password: ")
                                        .defaultValue("")
                                        .maskCharacter('*')
                                        .required()
                                        .and()
                                        .build().run();

                                String updatedPassword = passwordUpdateResult.getContext().get("password", String.class);
                                if (updatedPassword == null || updatedPassword.isBlank()) {
                                    ctx.outputWriter().println("Error: the updated password cannot be empty.");
                                } else {
                                    updatedCred.setPassword(updatedPassword);
                                }
                            }
                            case "additionalInfo" -> {
                                ComponentFlow.ComponentFlowResult additionalInfoSelectorResult;
                                if (updatedCred.getAdditionalInfo().isEmpty()) {
                                    additionalInfoSelectorResult = componentFlowBuilder.clone().reset()
                                            .withSingleItemSelector("additionalInfoActionSelector")
                                            .name("Choose what action you want to do")
                                            .selectItems(List.of(
                                                    SelectItem.of("Add New Info", "addInfo")
                                            ))
                                            .and()
                                            .build().run();
                                } else {
                                    List<SelectItem> infoItems = updatedCred.getAdditionalInfo().keySet().stream()
                                            .map(key -> SelectItem.of(key, key))
                                            .toList();
                                    additionalInfoSelectorResult = componentFlowBuilder.clone().reset()
                                            .withSingleItemSelector("additionalInfoActionSelector")
                                            .name("Choose what action you want to do")
                                            .selectItems(List.of(
                                                    SelectItem.of("Add New Info", "addInfo"),
                                                    SelectItem.of("Update Info",  "updateInfo"),
                                                    SelectItem.of("Remove Info",  "removeInfo")
                                            ))
                                            .next(selectorCtx -> selectorCtx.getResultItem().get().getItem().equals("addInfo")
                                                    ? null : "additionalInfoFieldSelector")
                                            .and()
                                            .withSingleItemSelector("additionalInfoFieldSelector")
                                            .name("Choose the field")
                                            .selectItems(infoItems)
                                            .and()
                                            .build().run();
                                }

                                String additionalInfoAction = additionalInfoSelectorResult.getContext()
                                        .get("additionalInfoActionSelector", String.class);

                                switch (additionalInfoAction) {
                                    case "addInfo" -> {
                                        boolean hasMoreInfo = true;
                                        while (hasMoreInfo) {
                                            ComponentFlow.ComponentFlowResult extraResult = componentFlowBuilder.clone()
                                                    .reset()
                                                    .withStringInput("key")
                                                    .name("Key:")
                                                    .required()
                                                    .and()
                                                    .withStringInput("value")
                                                    .name("Value:")
                                                    .required()
                                                    .and()
                                                    .withConfirmationInput("addAnother")
                                                    .name("Add another entry?")
                                                    .and()
                                                    .build()
                                                    .run();

                                            String key   = extraResult.getContext().get("key",   String.class);
                                            String value = extraResult.getContext().get("value", String.class);
                                            if (!updatedCred.addInfo(key, value)) {
                                                ctx.outputWriter().println("Error: the key '" + key + "' already exists. Use 'Update Info' to change its value.");
                                            }
                                            hasMoreInfo = extraResult.getContext().get("addAnother", Boolean.class);
                                        }
                                    }
                                    case "updateInfo" -> {
                                        String keyToUpdate = additionalInfoSelectorResult.getContext()
                                                .get("additionalInfoFieldSelector", String.class);
                                        ComponentFlow.ComponentFlowResult updateInfoResult = componentFlowBuilder.clone().reset()
                                                .withStringInput("newValue")
                                                .name("New value for '" + keyToUpdate + "'")
                                                .required()
                                                .and()
                                                .build().run();

                                        String newValue = updateInfoResult.getContext().get("newValue", String.class);
                                        if (!updatedCred.updateInfo(keyToUpdate, newValue)) {
                                            ctx.outputWriter().println("Error: failed to update the key '" + keyToUpdate + "'.");
                                        }
                                    }
                                    case "removeInfo" -> {
                                        String keyToRemove = additionalInfoSelectorResult.getContext()
                                                .get("additionalInfoFieldSelector", String.class);
                                        if (!updatedCred.removeInfo(keyToRemove)) {
                                            ctx.outputWriter().println("Error: failed to remove the key '" + keyToRemove + "'.");
                                        }
                                    }
                                }
                            }
                            case "exit" -> keepUpdating = false;
                        }

                        if (keepUpdating) {
                            ComponentFlow.ComponentFlowResult keepUpdatingResult = componentFlowBuilder.clone().reset()
                                    .withConfirmationInput("keepUpdating")
                                    .name("Do you want to keep updating this credential?")
                                    .and()
                                    .build().run();
                            keepUpdating = keepUpdatingResult.getContext().get("keepUpdating", Boolean.class);
                        }
                    }

                    try {
                        credentialService.updateCredential(serviceName, updatedCred);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Bean
    public Command deleteCredentialCommand() {
        return Command.builder()
                .name("delete")
                .description("Delete a credential")
                .help("A command to delete a credential. Usage: delete <service-name>")
                // no .options()
                .exitStatusExceptionMapper(exceptionMapper())
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    String serviceName = requireServiceName(ctx);
                    try {
                        credentialService.deleteCredential(serviceName);
                        ctx.outputWriter().println("Credential for '" + serviceName + "' deleted successfully.");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Bean
    public ExitStatusExceptionMapper exceptionMapper() {
        return exception -> {
            if (exception instanceof IllegalArgumentException)
                return new ExitStatus(1, exception.getMessage());
            else if (exception instanceof ResourceNotFoundException)
                return new ExitStatus(2, exception.getMessage());
            else
                return new ExitStatus(99, exception.getMessage());
        };
    }

    @Bean
    public AvailabilityProvider availabilityProvider() {
        return () -> authenticationService.isVaultUnlocked()
                ? Availability.available()
                : Availability.unavailable("You cannot use this command unless you have an unlocked vault.");
    }

    private void printCredential(CommandContext ctx, Credential cred) {
        ctx.outputWriter().printf(" username: %s%n password: %s%n", cred.getUsername(), cred.getPassword());
        cred.getAdditionalInfo().forEach((key, value) -> ctx.outputWriter().printf(" %s: %s%n", key, value));
    }
}
