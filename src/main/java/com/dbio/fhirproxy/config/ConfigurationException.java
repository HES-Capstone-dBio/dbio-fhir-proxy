package com.dbio.fhirproxy.config;

public class ConfigurationException extends Exception {

    private String action;

    private String reason;

    public ConfigurationException(String action, String reason) {
        this.action = action;
        this.reason = reason;
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }
}

