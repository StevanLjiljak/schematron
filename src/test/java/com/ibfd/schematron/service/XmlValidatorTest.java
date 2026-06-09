package com.ibfd.schematron.service;

import net.sf.saxon.s9api.Processor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class XmlValidatorTest {

    private XmlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new XmlValidator(new Processor(false));
    }

    @Test
    void givenActualArticleXml_whenValidate_thenNoException() throws Exception {
        try (InputStream xml = classpathStream("xml/wtj_2018_04_int_1.xml")) {
            assertDoesNotThrow(() -> validator.validate(xml));
        }
    }

    @Test
    void givenWellFormedXml_whenValidate_thenNoException() {
        byte[] xml = "<root><child>text</child></root>".getBytes();
        assertDoesNotThrow(() -> validator.validate(new ByteArrayInputStream(xml)));
    }

    @Test
    void givenMalformedXml_whenValidate_thenThrowsIllegalArgumentException() {
        byte[] malformed = "<unclosed><tag>".getBytes();
        assertThrows(IllegalArgumentException.class,
            () -> validator.validate(new ByteArrayInputStream(malformed)));
    }

    @Test
    void givenEmptyDocument_whenValidate_thenThrowsIllegalArgumentException() {
        byte[] empty = "".getBytes();
        assertThrows(IllegalArgumentException.class,
            () -> validator.validate(new ByteArrayInputStream(empty)));
    }

    private InputStream classpathStream(String path) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Resource not found on classpath: " + path);
        return stream;
    }
}
