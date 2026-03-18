package com.hameed.hameedpm.service.impl;

import com.hameed.hameedpm.model.Credential;
import com.hameed.hameedpm.service.Parser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CSVParserImpl implements Parser {
    @Override
    public List<Credential> parse(Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path);
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build().parse(reader)) {
            List<Credential> credentials = new ArrayList<>();
            for (CSVRecord csvRecord : csvParser) {
                Credential credential = getCredential(csvRecord);
                credentials.add(credential);
            }
            return credentials;
        } catch (IOException e) {
            throw new Exception("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }

    private Credential getCredential(CSVRecord csvRecord) {
        Credential credential = new Credential();
        credential.setServiceName(csvRecord.get(0));
        credential.setUsername(csvRecord.get(1));
        credential.setPassword(csvRecord.get(2));
        // Add any additional fields as needed from i=3 onwards
        Map<String, String> additionalInfo = new LinkedHashMap<>();
        int i = 3;
        while ((i + 1) <= csvRecord.size()) {
            String key = csvRecord.get(i).trim();
            String value = csvRecord.get(i + 1).trim();
            if (!key.isBlank()) additionalInfo.put(key, value);
            i += 2;
        }
        credential.setAdditionalInfo(additionalInfo);
        return credential;
    }
}
