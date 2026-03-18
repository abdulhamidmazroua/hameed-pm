package com.hameed.hameedpm;

import com.hameed.hameedpm.service.IVaultService;
import org.jline.reader.UserInterruptException;
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

}
