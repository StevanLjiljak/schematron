package com.ibfd.schematron.service;

import com.ibfd.schematron.model.PipelineResult;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class PipelineService {

    private static final String ARTICLE_XML = "xml/wtj_2018_04_int_1.xml";
    private static final String SCHEMATRON  = "schematron/pubdate-check.sch";

    private final XmlValidator       validator;
    private final XmlToHtmlConverter converter;
    private final SchematronRunner   schematronRunner;
    private final SvrlParser         svrlParser;

    private volatile PipelineResult cachedResult;

    public PipelineService(XmlValidator validator,
                           XmlToHtmlConverter converter,
                           SchematronRunner schematronRunner,
                           SvrlParser svrlParser) {
        this.validator        = validator;
        this.converter        = converter;
        this.schematronRunner = schematronRunner;
        this.svrlParser       = svrlParser;
    }

    public synchronized PipelineResult run() throws Exception {
        if (cachedResult == null) {
            cachedResult = execute();
        }
        return cachedResult;
    }

    private PipelineResult execute() throws Exception {
        byte[] xmlBytes = readClasspath(ARTICLE_XML);
        byte[] schBytes = readClasspath(SCHEMATRON);

        // Step 1 – well-formedness check (throws on invalid XML)
        validator.validate(new ByteArrayInputStream(xmlBytes));

        // Step 2 – XSLT transform
        ByteArrayOutputStream htmlOut = new ByteArrayOutputStream();
        converter.convert(new ByteArrayInputStream(xmlBytes), htmlOut);
        String articleHtml = htmlOut.toString(StandardCharsets.UTF_8);

        // Step 3 – Schematron validation
        ByteArrayOutputStream svrlOut = new ByteArrayOutputStream();
        schematronRunner.run(
                new ByteArrayInputStream(schBytes),
                new ByteArrayInputStream(xmlBytes),
                svrlOut);
        String svrlReport = svrlOut.toString(StandardCharsets.UTF_8);

        return new PipelineResult(
                articleHtml,
                svrlReport,
                svrlParser.parse(svrlReport));
    }

    private byte[] readClasspath(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Not found on classpath: " + path);
            return in.readAllBytes();
        }
    }
}
