package com.ibfd.schematron.service;

import net.sf.saxon.s9api.Processor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SchematronRunnerTest {

    private SchematronRunner runner;

    @BeforeEach
    void setUp() {
        runner = new SchematronRunner(new Processor(false));
    }

    // ── Number of failures ────────────────────────────────────────────────────

    @Test
    void report_containsExactlyTwoFailedAssertions() throws Exception {
        String svrl = runSchematron();
        // Count opening <svrl:failed-assert tags in the report
        int count = svrl.split("<svrl:failed-assert", -1).length - 1;
        assertThat(count)
            .as("Expected exactly 2 failed assertions in the SVRL report")
            .isEqualTo(2);
    }

    // ── Rule 1: <year> vs pubdate attribute year ──────────────────────────────

    @Test
    void report_containsYearMismatchFailure() throws Exception {
        String svrl = runSchematron();
        // <year>2019</year> does not match pubdate year 2018
        assertThat(svrl)
            .contains("2019")
            .contains("2018");
    }

    @Test
    void report_yearFailure_pointsToCorrectElement() throws Exception {
        String svrl = runSchematron();
        // The failure for the year rule must reference the <year> element
        assertThat(svrl).contains("year-vs-pubdate-attribute");
    }

    // ── Rule 2: pubdate attribute vs element text ─────────────────────────────

    @Test
    void report_containsPubdateDayMismatchFailure() throws Exception {
        String svrl = runSchematron();
        // Attribute says 2018-09-07 (day 7), text says "11 September 2018" (day 11)
        assertThat(svrl)
            .contains("2018-09-07")
            .contains("11 September 2018");
    }

    @Test
    void report_pubdateFailure_pointsToCorrectPattern() throws Exception {
        String svrl = runSchematron();
        assertThat(svrl).contains("pubdate-text-vs-attribute");
    }

    // ── Output is valid SVRL ──────────────────────────────────────────────────

    @Test
    void report_isValidSvrlDocument() throws Exception {
        String svrl = runSchematron();
        assertThat(svrl)
            .startsWith("<?xml")
            .contains("svrl:schematron-output")
            .contains("http://purl.oclc.org/dsdl/svrl");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String runSchematron() throws Exception {
        try (InputStream sch = classpathStream("schematron/pubdate-check.sch");
             InputStream xml = classpathStream("xml/wtj_2018_04_int_1.xml")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            runner.run(sch, xml, out);
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private InputStream classpathStream(String path) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Resource not found on classpath: " + path);
        return stream;
    }
}
