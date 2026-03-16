package com.hameed.hameedpm.commands;

import com.hameed.hameedpm.exception.ResourceNotFoundException;
import com.hameed.hameedpm.model.Credential;
import com.hameed.hameedpm.service.ICredentialService;
import com.hameed.hameedpm.service.impl.IAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.availability.AvailabilityProvider;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;
import org.springframework.shell.jline.tui.component.SingleItemSelector;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Configuration
public class CommandsConfig {

    private final IAuthenticationService authenticationService;
    private final ICredentialService credentialService;
    private final ComponentFlow.Builder componentFlowBuilder;

    @Autowired
    public CommandsConfig(IAuthenticationService authenticationService, ICredentialService credentialService, ComponentFlow.Builder componentFlowBuilder) {
        this.authenticationService = authenticationService;
        this.credentialService = credentialService;
        this.componentFlowBuilder = componentFlowBuilder;
    }

    @Bean
    public Command addCredentialsCommand() {
        return Command.builder()
                .name("add")
                .description("Add a new credential")
                .help("Adds a new credential. Usage: add [-s | --service-name] <service-name>]")
                .options(CommandOption.with()
                        .longName("service-name")
                        .shortName('s')
                        .description("The service name")
                        .required(true)
                        .type(String.class)
                        .build())
                .exitStatusExceptionMapper(exceptionMapper())
//                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    CommandOption serviceNameOption = ctx.getOptionByShortName('s');
                    if (serviceNameOption == null || serviceNameOption.value() == null || serviceNameOption.value().isBlank()) {
                        throw new IllegalArgumentException("service-name is missing \nUsage: add [-s | --service-name] <service-name>");
                    }

                    String serviceName = serviceNameOption.value();
                    // Step 1: Collect username and password via ComponentFlow.
                    // StringInput with a mask character is the official Spring Shell 4.x
                    // way to securely prompt for a password — no raw readPassword() needed.
                    ComponentFlow.ComponentFlowResult basicResult = componentFlowBuilder.clone()
                            .reset()
                            .withStringInput("username")
                            .name("Username / Email")
                            .required()
                            .and()
                            .withStringInput("password")
                            .name("Password")
                            .maskCharacter('*')
                            .required()
                            .and()
                            .withConfirmationInput("hasMoreInfo")
                            .name("Do you want to add extra info?")
                            .and()
                            .build()
                            .run();

                    String username = basicResult.getContext().get("username", String.class);
                    String password = basicResult.getContext().get("password", String.class);
                    boolean hasMoreInfo = basicResult.getContext().get("hasMoreInfo", Boolean.class);

                    // Step 2: Collect additional key/value pairs in a loop if requested.
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
                        String value = extraResult.getContext().get("value",  String.class);
                        additionalInfo.put(key, value);
                        hasMoreInfo = extraResult.getContext().get("addAnother", Boolean.class);
                    }

                    // Step 3: Delegate to your service.
                    credentialService.addCredential(
                            serviceName,
                            username,
                            password.toCharArray(),   // keep char[] for security
                            additionalInfo.isEmpty() ? null : additionalInfo
                    );

                    ctx.outputWriter().println("Credential for '" + serviceName + "' saved successfully.");
                });
    }

    @Bean
    public Command listCredentialsCommand() {
        return Command.builder()
                .name("list")
                .description("list credentials")
                .help("A command that lists all credentials in the vault. Usage: list [-d | --detailed]")
                .options(CommandOption.with()
                        .shortName('d')
                        .longName("detailed")
                        .required(false)
                        .type(boolean.class)
                        .build())
//                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    CommandOption detailedOption = ctx.getOptionByShortName('d');
                    boolean detailed = detailedOption != null &&
                            detailedOption.value() != null &&
                            Boolean.parseBoolean(detailedOption.value());
                    if (detailed)
                        credentialService.listCredentials()
                                .forEach(cred -> {
                                    printCredential(ctx, cred);
                                });
                    else {
                        List<Credential> credentials = credentialService.listCredentials();
                        for (int i=0; i < credentials.size(); i++) {
                            ctx.outputWriter().println(i + ". " + credentials.get(i).getServiceName());
                        }
                    }
                });
    }

    @Bean
    public Command getCredentialCommand() {
        return Command.builder()
                .name("get")
                .description("get specific credential details")
                .help("A command that displays the credential details. Usage: get [-s | --service-name] <service-name>")
                .options(CommandOption.with()
                        .shortName('s')
                        .longName("service-name")
                        .required(true)
                        .build())
                .exitStatusExceptionMapper(exceptionMapper())
//                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    CommandOption serviceNameOption = ctx.getOptionByShortName('s');
                    if (serviceNameOption == null || serviceNameOption.value() == null || serviceNameOption.value().isBlank()) {
                        throw new IllegalArgumentException("service-name is missing \nUsage: get [-s | --service-name] <service-name>");
                    }
                    String serviceName = serviceNameOption.value();
                    Credential cred = credentialService.getCredentialByServiceName(serviceName).orElseThrow(() -> new ResourceNotFoundException("Credential with this service name was not found: " + serviceName));
                    printCredential(ctx, cred);
                });
    }

    @Bean
    public Command updateCredentialCommand() {
        return Command.builder()
                .name("update")
                .description("update specific credential details")
                .help("A command to update credential details. Usage: update [-s | --service-name] <service-name>")
                .options(CommandOption.with()
                        .longName("service-name")
                        .shortName('s')
                        .description("The service name")
                        .required(true)
                        .type(String.class)
                        .build())
                .exitStatusExceptionMapper(exceptionMapper())
//                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    CommandOption serviceNameOption = ctx.getOptionByShortName('s');
                    if (serviceNameOption == null || serviceNameOption.value() == null || serviceNameOption.value().isBlank()) {
                        throw new IllegalArgumentException("service-name is missing \nUsage: add [-s | --service-name] <service-name>");
                    }

                    String serviceName = serviceNameOption.value();

                    Credential cred = credentialService.getCredentialByServiceName(serviceName).orElseThrow(() -> new ResourceNotFoundException("Credential with this service name was not found: " + serviceName));
                    // clone using the copy constructor
                    Credential updatedCred = new Credential(cred);
                    printCredential(ctx, cred);
                    boolean keepUpdating = true;

                    while (keepUpdating) {
                        ComponentFlow.ComponentFlowResult updateFieldSelectorResult = componentFlowBuilder.clone()
                                .reset()
                                .withSingleItemSelector("updateFieldSelector")
                                .name("Choose what you want to update:")
                                .selectItems(List.of(
                                        SelectItem.of("Username / Email: ", "username"),
                                        SelectItem.of("Password", "password"),
                                        SelectItem.of("Additional Info", "additionalInfo")
                                )).and().build().run();

                        String fieldToUpdate = updateFieldSelectorResult.getContext().get("updateFieldSelector", String.class);

                        switch (fieldToUpdate) {
                            case "username" -> {
                                ComponentFlow.ComponentFlowResult userUpdateResult = componentFlowBuilder.clone().reset()
                                        .withStringInput("username")
                                        .name("Updated Username / Email: ")
                                        .defaultValue("")
                                        .required()
                                        .and().build().run();

                                String updatedUsername = userUpdateResult.getContext().get("username", String.class);
//                                if (updatedUsername.isBlank()) throw new IllegalArgumentException("the updated username cannot be empty");
                                if (updatedUsername.isBlank()) {
                                    ctx.outputWriter().println("Error: the updated username cannot be empty");
                                    continue;
                                }
                                updatedCred.setUsername(updatedUsername);
                            }
                            case "password" -> {
                                ComponentFlow.ComponentFlowResult passwordUpdateResult = componentFlowBuilder.clone().reset()
                                        .withStringInput("password")
                                        .name("Updated Password: ")
                                        .defaultValue("")
                                        .maskCharacter('*')
                                        .required()
                                        .and().build().run();

                                String updatePassword = passwordUpdateResult.getContext().get("password", String.class);
//                                if (updatePassword.isBlank()) throw new IllegalArgumentException("the updated password cannot be empty");
                                if (updatePassword.isBlank()) {
                                    ctx.outputWriter().println("Error: the updated password cannot be empty");
                                    continue;
                                }
                                updatedCred.setPassword(updatePassword);
                            }
                            case "additionalInfo" -> {
                                ComponentFlow.ComponentFlowResult additionalInfoSelectorResult;
                                if (updatedCred.getAdditionalInfo().isEmpty()) {
                                    additionalInfoSelectorResult = componentFlowBuilder.clone().reset()
                                            .withSingleItemSelector("additionalInfoActionSelector")
                                            .name("Choose what action you want to do")
                                            .selectItems(List.of(
                                                    SelectItem.of("Add New Info", "addInfo")
                                            )).and().build().run();
                                } else {
                                    // get a list of items of the additional infofirst
                                    List<SelectItem> infoItems = updatedCred.getAdditionalInfo().keySet().stream()
                                            .map(key -> SelectItem.of(key, key))
                                            .toList();
                                    additionalInfoSelectorResult = componentFlowBuilder.clone().reset()
                                            .withSingleItemSelector("additionalInfoActionSelector")
                                            .name("Choose what action you want to do")
                                            .selectItems(List.of(
                                                    SelectItem.of("Add New Info", "addInfo"),
                                                    SelectItem.of("Update Info", "updateInfo"),
                                                    SelectItem.of("Remove Info", "removeInfo")
                                            )).next(selectorContext -> selectorContext.getResultItem().get().getItem().equals("addInfo") ? null : "additionalInfoFieldSelector")
                                            .and()
                                            .withSingleItemSelector("additionalInfoFieldSelector")
                                            .name("Choose the field")
                                            .selectItems(infoItems)
                                            .and().build().run();
                                }

                                String additionalInfoAction = additionalInfoSelectorResult.getContext().get("additionalInfoActionSelector", String.class);
                                switch (additionalInfoAction) {
                                    case "addInfo" -> {
                                        boolean hasMoreInfo = true;
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

                                            String key = extraResult.getContext().get("key", String.class);
                                            String value = extraResult.getContext().get("value", String.class);
                                            updatedCred.addInfo(key, value);
                                            hasMoreInfo = extraResult.getContext().get("addAnother", Boolean.class);
                                        }
                                    }
                                    case "updateInfo" -> {
                                        // update info
                                        String keyToUpdate = additionalInfoSelectorResult.getContext().get("additionalInfoFieldSelector", String.class);
                                        ComponentFlow.ComponentFlowResult updateInfoResult = componentFlowBuilder.clone().reset()
                                                .withStringInput("newValue")
                                                .name("New value for " + keyToUpdate)
                                                .required()
                                                .and().build().run();

                                        String newValue = updateInfoResult.getContext().get("newValue", String.class);
                                        updatedCred.updateInfo(keyToUpdate, newValue);
                                    }
                                    case "removeInfo" -> {
                                        // remove info
                                        String keyToRemove = additionalInfoSelectorResult.getContext().get("additionalInfoFieldSelector", String.class);
                                        updatedCred.removeInfo(keyToRemove);
                                    }
                                    default -> {
                                        // should not reach here
                                    }
                                }
                            }
                        }

                        // keep updating ?
                        ComponentFlow.ComponentFlowResult keepUpdatingResult = componentFlowBuilder.clone().reset()
                                .withConfirmationInput("keepUpdating")
                                .name("Do you want to keep updating this credential?")
                                .and().build().run();
                        keepUpdating = keepUpdatingResult.getContext().get("keepUpdating", Boolean.class);
                    }

                    if(credentialService.updateCredential(serviceName, updatedCred)) {
                        ctx.outputWriter().println("Credential for '" + serviceName + "' updated successfully.");
                    } else {
                        ctx.outputWriter().println("Failed to update the credential for '" + serviceName + "'.");
                    }
                });
    }


    @Bean
    public Command deleteCredentialCommand() {
        return Command.builder()
                .name("delete")
                .description("delete a credential")
                .help("A command to delete a credential. Usage: delete [-s | --service-name] <service-name>")
                .options(CommandOption.with()
                        .longName("service-name")
                        .shortName('s')
                        .description("The service name")
                        .required(true)
                        .type(String.class)
                        .build())
                .exitStatusExceptionMapper(exceptionMapper())
//                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    CommandOption serviceNameOption = ctx.getOptionByShortName('s');
                    if (serviceNameOption == null || serviceNameOption.value() == null || serviceNameOption.value().isBlank()) {
                        throw new IllegalArgumentException("service-name is missing \nUsage: delete [-s | --service-name] <service-name>");
                    }
                    String serviceName = serviceNameOption.value();
                    if(credentialService.deleteCredential(serviceName)) {
                        ctx.outputWriter().println("Credential for '" + serviceName + "' deleted successfully.");
                    } else {
                        ctx.outputWriter().println("Failed to delete the credential for '" + serviceName + "'. It may not exist.");
                    }
                });


    }



    @Bean
    public Command testCommand() {
        return Command.builder()
                .name("test")
                .execute(ctx -> {
                    ComponentFlow.ComponentFlowResult actionResult = componentFlowBuilder.clone().reset()
                            .withSingleItemSelector("action")
                            .name("What do you want to do")
                            .selectItems(List.of(
                                    SelectItem.of("Update credential", "update"),
                                    SelectItem.of("Remove credential", "delete")
                            )).and().build().run();

                    String action = actionResult.getContext().get("action", String.class);

                    switch (action) {
                        case "update" -> {
                            ComponentFlow.ComponentFlowResult updateResult = componentFlowBuilder.clone().reset()
                                    .withStringInput("update")
                                    .name("username: ")
                                    .and()
                                    .build().run();
                            String serviceToUpdate = updateResult.getContext().get("update", String.class);
                            if (!serviceToUpdate.isBlank())
                                ctx.outputWriter().println("update required for this service: " + serviceToUpdate);
                        }
                        case "delete" -> {
                            ComponentFlow.ComponentFlowResult deleteResult = componentFlowBuilder.clone().reset()
                                    .withStringInput("delete")
                                    .name("which service you want to delete: ")
                                    .defaultValue("")
                                    .and()
                                    .build().run();

                            String serviceToDelete = deleteResult.getContext().get("delete", String.class);
                            if(!serviceToDelete.isBlank())
                                ctx.outputWriter().println("delete required for this service: " + serviceToDelete);
                        }
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
                return new ExitStatus(99,exception.getMessage());
        };
    }

    @Bean
    public AvailabilityProvider availabilityProvider() {
        return () -> authenticationService.isAuthenticated() ? Availability.available() : Availability.unavailable("You cannot use this command unless you have a vault. To create a vault, " +
                "use the following command: create vault");
    }

    private void printCredential(CommandContext ctx, Credential cred) {
        ctx.outputWriter().printf(" username: %s%n password: %s%n", cred.getUsername(), cred.getPassword());
        cred.getAdditionalInfo().forEach((key, value) -> ctx.outputWriter().printf(" %s: %s%n", key, value));
    }
}
