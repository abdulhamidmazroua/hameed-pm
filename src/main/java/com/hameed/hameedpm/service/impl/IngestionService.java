package com.hameed.hameedpm.service.impl;

import com.hameed.hameedpm.enums.TemplateType;
import com.hameed.hameedpm.model.Credential;
import com.hameed.hameedpm.service.IIngestionService;
import com.hameed.hameedpm.service.Parser;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class IngestionService implements IIngestionService {


    private final CredentialService credentialService;
    private Map<TemplateType, Parser> parserMap;

    public IngestionService(CredentialService credentialService) {
        this.credentialService = credentialService;
        this.parserMap = Map.of(
            TemplateType.CSV, new CSVParserImpl()
        );
    }

    @Override
    public void ingest(String filePath, CommandContext ctx) throws Exception {
        Path path = Paths.get(filePath);
        List<Credential> credentials = parserMap.get(determineTemplateType(path)).parse(path);
        credentialService.addAll(credentials, ctx);
    }

    @Override
    public void getTemplate(TemplateType templateType) {
        Path templatePath = Paths.get("credential_template." + templateType.getFileExtensions()[0]);
        try {
            Files.writeString(templatePath, templateType.getTemplateHeader());
            System.out.println("Template created at: " + templatePath.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create template file: " + e.getMessage(), e);
        }
    }

    private TemplateType determineTemplateType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        // extensions now are a string attached to the enum type as an array of strings
        return Arrays.stream(TemplateType.values())
                .filter(type -> Arrays.stream(type.getFileExtensions()).anyMatch(fileName::endsWith))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type for ingestion: " + fileName));
    }
}
