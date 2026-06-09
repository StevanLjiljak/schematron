package com.ibfd.schematron.service;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 * Validates XML well-formedness using Saxon's document builder.
 * A parse exception is thrown (and re-thrown as IllegalArgumentException)
 * if the document is not well-formed.
 */
@Service
public class XmlValidator {

    private final Processor processor;

    public XmlValidator(Processor processor) {
        this.processor = processor;
    }

    public void validate(InputStream xmlStream) {
        DocumentBuilder builder = processor.newDocumentBuilder();
        try {
            builder.build(new StreamSource(xmlStream));
        } catch (SaxonApiException e) {
            throw new IllegalArgumentException("XML validation failed: " + e.getMessage(), e);
        }
    }
}
