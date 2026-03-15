package com.hameed.hameedpm;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.jline.PromptProvider;

@SpringBootApplication
public class HameedPM {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(HameedPM.class, args);
    }

    @Bean
    public PromptProvider myPromptProvider() {
        return () -> new AttributedString("hameed-pm:>",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
    }

    @Bean
    public Command sayHelloProgrammatically() {
        return Command.builder()
                .name("hello-prog")
                .group("Greetings")
                .description("Say hello without a name but from a defined command bean")
                .execute(commandContext -> {
                    commandContext.outputWriter().println("Hello Programmatically");
                });
    }



}
