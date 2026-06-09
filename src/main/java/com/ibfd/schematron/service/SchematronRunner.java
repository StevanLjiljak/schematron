package com.ibfd.schematron.service;

import net.sf.saxon.s9api.*;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;

/**
 * Runs ISO Schematron validation in two steps using Saxon and SchXslt2:
 *
 *   Step 1 – transpile:  .sch  →  check.xsl
 *            (equivalent to: java -jar saxon-he.jar -xsl:transpile.xsl -s:<sch> -o:check.xsl)
 *
 *   Step 2 – validate:   XML   →  report.xml   (SVRL)
 *            (equivalent to: java -jar saxon-he.jar -xsl:check.xsl -s:<xml> -o:report.xml)
 *
 * SchXslt2 setup:
 *   Download the release zip from https://codeberg.org/schxslt/schxslt2/releases
 *   and place transpile.xsl at src/main/resources/schxslt2/transpile.xsl.
 *   (Already bundled in this project from v1.10.3.)
 */
@Service
public class SchematronRunner {

    // SchXslt2 v1.10.3 ships transpile.xsl at the archive root (no sub-directory)
    private static final String TRANSPILE_XSL = "schxslt2/transpile.xsl";

    private final Processor processor;

    public SchematronRunner(Processor processor) {
        this.processor = processor;
    }

    /**
     * @param schStream    Schematron .sch file as a stream
     * @param xmlStream    Source XML file to validate
     * @param reportStream Target stream that will receive the SVRL report XML
     */
    public void run(InputStream schStream, InputStream xmlStream, OutputStream reportStream)
            throws SaxonApiException, IOException {

        XsltCompiler compiler = processor.newXsltCompiler();

        // ── Step 1: transpile .sch → XSLT (held in memory) ──────────────────
        URL transpileUrl = resolveClasspathResource(TRANSPILE_XSL);
        XsltExecutable transpileExec = compiler.compile(new StreamSource(transpileUrl.toString()));
        Xslt30Transformer transpiler = transpileExec.load30();

        ByteArrayOutputStream checkXslBytes = new ByteArrayOutputStream();
        Serializer checkXslSerializer = processor.newSerializer(checkXslBytes);
        transpiler.applyTemplates(new StreamSource(schStream), checkXslSerializer);

        // ── Step 2: validate XML using the generated XSLT ────────────────────
        XsltExecutable checkExec = compiler.compile(
            new StreamSource(new ByteArrayInputStream(checkXslBytes.toByteArray()))
        );
        Xslt30Transformer checker = checkExec.load30();

        Serializer reportSerializer = processor.newSerializer(reportStream);
        checker.applyTemplates(new StreamSource(xmlStream), reportSerializer);
    }

    private URL resolveClasspathResource(String path) {
        URL url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            throw new IllegalStateException(
                "Classpath resource not found: " + path + "\n" +
                "Download transpile.xsl from https://codeberg.org/schxslt/schxslt2/releases " +
                "and place it at src/main/resources/schxslt2/transpile.xsl."
            );
        }
        return url;
    }
}
