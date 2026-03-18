package com.hameed.hameedpm.service;

import com.hameed.hameedpm.enums.TemplateType;
import org.springframework.shell.core.command.CommandContext;

import java.io.IOException;

public interface IIngestionService {
    void ingest(String filePath, CommandContext ctx) throws Exception;
    void getTemplate(TemplateType templateType);
}
