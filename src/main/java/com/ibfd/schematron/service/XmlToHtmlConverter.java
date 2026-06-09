package com.ibfd.schematron.service;

import net.sf.saxon.s9api.*;
import org.springframework.beans.factory.annotation.Value;
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
 * The collections lookup URL is configurable via application.properties
 * (collections.url). By default it points to the live IBFD URL; set it to
 * "classpath:config/collections.xml" to use the bundled local copy instead.
 */
@Service
public class XmlToHtmlConverter {

    private final Processor processor;
    private final String collectionsUrl;

    public XmlToHtmlConverter(Processor processor,
                               @Value("${collections.url}") String collectionsUrl) {
        this.processor      = processor;
        this.collectionsUrl = collectionsUrl;
    }

    public void convert(InputStream xmlStream, OutputStream htmlStream) throws SaxonApiException {
        XsltCompiler compiler = processor.newXsltCompiler();

        URL xsltUrl = resolveClasspathResource("xslt/article-to-html.xsl");
        XsltExecutable executable = compiler.compile(new StreamSource(xsltUrl.toString()));

        Xslt30Transformer transformer = executable.load30();

        // Pass the collections URL as an XSLT parameter.
        // Saxon's doc() can resolve both file:// (local) and https:// (live) URIs.
        transformer.setStylesheetParameters(Map.of(
            new QName("collectionsUrl"), new XdmAtomicValue(collectionsUrl)
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
