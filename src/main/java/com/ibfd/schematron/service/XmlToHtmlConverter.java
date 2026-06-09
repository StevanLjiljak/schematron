package com.ibfd.schematron.service;

import net.sf.saxon.s9api.*;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;

/**
 * Converts an IBFD journal XML article to an HTML document outline using
 * Saxon XSLT 2.0 and the bundled article-to-html.xsl stylesheet.
 *
 * The stylesheet receives the path to the local collections.xml so it can
 * look up the collection name without a live HTTP fetch.
 */
@Service
public class XmlToHtmlConverter {

    private final Processor processor;

    public XmlToHtmlConverter(Processor processor) {
        this.processor = processor;
    }

    public void convert(InputStream xmlStream, OutputStream htmlStream) throws SaxonApiException {
        XsltCompiler compiler = processor.newXsltCompiler();

        URL xsltUrl = resolveClasspathResource("xslt/article-to-html.xsl");
        XsltExecutable executable = compiler.compile(new StreamSource(xsltUrl.toString()));

        Xslt30Transformer transformer = executable.load30();

        // Pass collections.xml location as a stylesheet parameter so that
        // the XSLT doc() call resolves to a local file (no live HTTP needed)
        URL collectionsUrl = resolveClasspathResource("config/collections.xml");
        transformer.setStylesheetParameters(Map.of(
            new QName("collectionsUrl"), new XdmAtomicValue(collectionsUrl.toString())
        ));

        Serializer serializer = processor.newSerializer(htmlStream);
        transformer.applyTemplates(new StreamSource(xmlStream), serializer);
    }

    private URL resolveClasspathResource(String path) {
        URL url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            throw new IllegalStateException("Classpath resource not found: " + path);
        }
        return url;
    }
}
