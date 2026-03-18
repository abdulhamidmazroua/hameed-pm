package com.hameed.hameedpm.service;

import com.hameed.hameedpm.model.Credential;

import java.nio.file.Path;
import java.util.List;

public interface Parser {
    List<Credential> parse(Path path) throws Exception;
}
