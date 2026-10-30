package org.acme.reporter;

public record StartupContext(
    String port, String root, String reports, String swagger, String openApi) {}
