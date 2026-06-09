package com.ibfd.schematron.model;

public record FailedAssertion(
        String patternId,
        String test,
        String location,
        String message
) {}
