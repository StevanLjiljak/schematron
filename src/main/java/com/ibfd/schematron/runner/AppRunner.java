package com.ibfd.schematron.runner;

import com.ibfd.schematron.service.SchematronRunner;
import com.ibfd.schematron.service.XmlToHtmlConverter;
import com.ibfd.schematron.service.XmlValidator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;

/**
 * Orchestrates the full pipeline on application startup:
 *
 *   1. Validate  wtj_2018_04_int_1.xml  (well-formedness)
 *   2. Transform XML → output/article.html  (XSLT Part 1)
 *   3. Validate  pubdate rules → output/report.xml  (Schematron Part 2)
 *
 * Output files are written relative to the working directory.
 */
@Component
public class AppRunner implements ApplicationRunner {

    private static final String ARTICLE_XML = "xml/wtj_2018_04_int_1.xml";
    private static final String SCHEMATRON  = "schematron/pubdate-check.sch";

    private final XmlValidator       validator;
    private final XmlToHtmlConverter converter;
    private final SchematronRunner   schematronRunner;

    public AppRunner(XmlValidator validator,
                     XmlToHtmlConverter converter,
                     SchematronRunner schematronRunner) {
        this.validator        = validator;
        this.converter        = converter;
        this.schematronRunner = schematronRunner;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path outputDir = Path.of("output");
        outputDir.toFile().mkdirs();

        // ── 1. XML validation ────────────────────────────────────────────────
        System.out.println("=== Step 1: Validating XML ===");
        try (InputStream xml = classpathStream(ARTICLE_XML)) {
            validator.validate(xml);
            System.out.println("    XML is well-formed.");
        } catch (IllegalArgumentException e) {
            System.err.println("    " + e.getMessage());
        }

        // ── 2. XSLT: XML → HTML ──────────────────────────────────────────────
        System.out.println("=== Step 2: Transforming XML to HTML ===");
        File htmlFile = outputDir.resolve("article.html").toFile();
        try (InputStream xml  = classpathStream(ARTICLE_XML);
             OutputStream html = new FileOutputStream(htmlFile)) {
            converter.convert(xml, html);
            System.out.println("    HTML written to " + htmlFile.getAbsolutePath());
        }

        // ── 3. Schematron validation ─────────────────────────────────────────
        System.out.println("=== Step 3: Running Schematron validation ===");
        File reportFile = outputDir.resolve("report.xml").toFile();
        try (InputStream sch  = classpathStream(SCHEMATRON);
             InputStream xml  = classpathStream(ARTICLE_XML);
             OutputStream rpt = new FileOutputStream(reportFile)) {
            schematronRunner.run(sch, xml, rpt);
            System.out.println("    SVRL report written to " + reportFile.getAbsolutePath());
            System.out.println("    (The report should contain 2 failed assertions.)");
        } catch (IllegalStateException e) {
            // SchXslt2 files not yet placed on classpath – print setup instructions
            System.err.println("    Schematron step skipped: " + e.getMessage());
        }
    }

    private InputStream classpathStream(String path) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Resource not found on classpath: " + path);
        }
        return stream;
    }
}
