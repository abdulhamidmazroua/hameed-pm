package com.hameed.hameedpm.commands;

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
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;

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
                .availabilityProvider(availabilityProvider())
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
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    if (ctx.getOptionByShortName('d').value() != null && Boolean.parseBoolean(ctx.getOptionByShortName('d').value()))
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
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    CommandOption serviceNameOption = ctx.getOptionByShortName('s');
                    if (serviceNameOption == null || serviceNameOption.value() == null || serviceNameOption.value().isBlank()) {
                        throw new IllegalArgumentException("service-name is missing \nUsage: get [-s | --service-name] <service-name>");
                    }
                    String serviceName = serviceNameOption.value();
                    Credential cred = credentialService.getCredentialByServiceName(serviceName);
                    if (cred == null) {
                        ctx.outputWriter().println("No credential for this service name: " + serviceName);
                        return;
                    }
                    printCredential(ctx, cred);
                });
    }

//    @Bean
//    public Command updateCredentialCommand() {
//        return Command.builder()
//                .name("update")
//                .description("update specific credential details")
//                .help("A command to update credential details. Usage: update")
//                .options(CommandOption.with()
//                        .required(true)
//                        .build());
//
//    }

    @Bean
    public ExitStatusExceptionMapper exceptionMapper() {
        return exception -> {
            if (exception instanceof IllegalArgumentException)
                return new ExitStatus(1, "Error with exit code: \n" + exception.getMessage());
            else
                return new ExitStatus(99, "Internal Error, unexpected: \n" + exception.getMessage());
        };
    }

    @Bean
    public AvailabilityProvider availabilityProvider() {
        return () -> authenticationService.isAuthenticated() ? Availability.available() : Availability.unavailable("You cannot use this command unless you have a vault. To create a vault, " +
                "use the following command: create vault");
    }

    private void printCredential(CommandContext ctx, Credential cred) {
        ctx.outputWriter().printf(" username: %s%n password: %s%n", cred.getUsername(), cred.getPassword());
        cred.getAdditionalInfo().forEach((key, value) -> ctx.outputWriter().printf(" %s: %s%n %s: %s%n", key, value));
    }
}
