import com.fasterxml.jackson.databind.ObjectMapper;
import service.AuthenticationService;
import service.OrchestrationService;

import java.io.Console;
import java.util.Scanner;

public class PasswordManager {

    public static void main(String[] args) throws Exception {

        // manage dependencies
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        AuthenticationService authenticationService = new AuthenticationService(mapper);
        Scanner input = new Scanner(System.in);
        Console console = System.console();
        OrchestrationService orchestrationService = new OrchestrationService(mapper, authenticationService, input, console);

        // shutdown gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            System.out.println("Shutting down... cleaning resources");
            // close files, and cleanup everything
            try {
                orchestrationService.cleanup();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        // run
        orchestrationService.run(args);
    }
}
