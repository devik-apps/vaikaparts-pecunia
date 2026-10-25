package com.devikapps.vaikaparts.client.papi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PapiErrorResponse(String code, String source, String message) {}
