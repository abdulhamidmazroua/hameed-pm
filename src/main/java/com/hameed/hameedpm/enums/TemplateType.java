package com.hameed.hameedpm.enums;

public enum TemplateType {
    CSV(
            "service_name,username,password,key1,value1,key2,value2,key3,value3",
            "csv"
    );

    private final String templateHeader;
    private final String[] fileExtensions;

    TemplateType(String templateHeader, String... fileExtensions) {
        this.templateHeader = templateHeader;
        this.fileExtensions = fileExtensions;
    }

    public String getTemplateHeader() {
        return templateHeader;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

}
