package com.ibfd.schematron.service;

import net.sf.saxon.s9api.Processor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XmlToHtmlConverterTest {

    private XmlToHtmlConverter converter;

    @BeforeEach
    void setUp() {
        // Use the bundled local copy so tests never require a network connection
        String collectionsUrl = Objects.requireNonNull(
            getClass().getClassLoader().getResource("config/collections.xml"),
            "collections.xml not found on classpath"
        ).toString();

        converter = new XmlToHtmlConverter(new Processor(false), collectionsUrl);
    }

    // ── HTML <title> ─────────────────────────────────────────────────────────

    @Test
    void htmlTitle_containsArticleTitle() {
        String html = convert();
        assertThat(html).contains("What Is Really Wrong with Global Tax Governance and How to Properly Fix It");
    }

    @Test
    void htmlTitle_containsCollectionNameFromLookup() {
        String html = convert();
        assertThat(html).contains("World Tax Journal");
    }

    @Test
    void htmlTitle_isConcatenationOfArticleTitleAndCollectionName() {
        String html = convert();
        assertThat(html).contains(
            "What Is Really Wrong with Global Tax Governance and How to Properly Fix It - World Tax Journal"
        );
    }

    // ── Section → heading depth ───────────────────────────────────────────────

    @Test
    void topLevelSections_renderedAsH1() {
        String html = convert();
        // Section 1 "Introduction" is top-level → h1
        assertThat(html).contains("<h1>");
        assertThat(html).contains("Introduction");
    }

    @Test
    void nestedSections_renderedAsH2() {
        String html = convert();
        // Sections 2.1, 2.2, 3.1, 3.2, 4.1, 4.2, 4.3 are one level deep → h2
        assertThat(html).contains("<h2>");
        assertThat(html).contains("Expertise as a source of misrepresentation");
    }

    @Test
    void doublyNestedSections_renderedAsH3() {
        String html = convert();
        // Sections 3.2.1 and 3.2.2 are two levels deep → h3
        assertThat(html).contains("<h3>");
        assertThat(html).contains("Who actually needs an international tax court?");
    }

    @Test
    void sectionLabel_wrappedInLabelSpan() {
        String html = convert();
        assertThat(html).contains("<span class=\"label\">1. </span>");
    }

    @Test
    void sectionTitle_wrappedInTitleSpan() {
        String html = convert();
        assertThat(html).contains("<span class=\"title\">");
    }

    // ── Inline markup inside <title> ─────────────────────────────────────────

    @Test
    void subTagInTitle_preservedAsHtmlSub() {
        // Section 3: "...The Problem of CO<sub>2</sub>"
        String html = convert();
        assertThat(html).contains("CO<sub>2</sub>");
    }

    @Test
    void footnoteInTitle_textSuppressed() {
        // Section 3.2.1 title has <footnote>See ITO, op. cit.</footnote> inline
        String html = convert();
        assertThat(html).contains("Who actually needs an international tax court?");
        assertThat(html).doesNotContain("See ITO, op. cit.");
    }

    @Test
    void emphTypeI_renderedAsItalicTag() {
        // Section 4.2: <emph type="i">Smith vs. Jones</emph>
        String html = convert();
        assertThat(html).contains("<i>Smith vs. Jones</i>");
    }

    // ── Body text exclusion ───────────────────────────────────────────────────

    @Test
    void bodyParagraphText_notPresentInOutput() {
        // Opening sentence of section 1 body — must NOT appear in the outline HTML
        String html = convert();
        assertThat(html).doesNotContain("Time and time again, the richest");
    }

    @Test
    void footnoteBodyText_notPresentInOutput() {
        // Footnote 1 body text — must NOT appear anywhere in the HTML
        String html = convert();
        assertThat(html).doesNotContain("Global Tax Governance: What It Is and Why It Matters");
    }

    @Test
    void allFiveSections_presentInOutput() {
        String html = convert();
        assertThat(html)
            .contains("1. ")
            .contains("2. ")
            .contains("3. ")
            .contains("4. ")
            .contains("5. ");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String convert() {
        try (InputStream xml = getClass().getClassLoader().getResourceAsStream("xml/wtj_2018_04_int_1.xml")) {
            assertNotNull(xml, "Article XML not found on classpath");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            converter.convert(xml, out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Conversion failed in test", e);
        }
    }
}
