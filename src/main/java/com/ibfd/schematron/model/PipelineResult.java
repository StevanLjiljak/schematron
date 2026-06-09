package com.ibfd.schematron.model;

import java.util.List;

public record PipelineResult(
        String articleHtml,
        String svrlReport,
        List<FailedAssertion> failedAssertions
) {}
