package com.ibfd.schematron.service;

import com.ibfd.schematron.model.FailedAssertion;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class SvrlParser {

    private static final String SVRL_NS = "http://purl.oclc.org/dsdl/svrl";

    private final Processor processor;

    public SvrlParser(Processor processor) {
        this.processor = processor;
    }

    public List<FailedAssertion> parse(String svrl) throws SaxonApiException {
        XPathCompiler xpc = processor.newXPathCompiler();
        xpc.declareNamespace("svrl", SVRL_NS);

        XdmNode doc = processor.newDocumentBuilder()
                .build(new StreamSource(new StringReader(svrl)));

        List<FailedAssertion> result = new ArrayList<>();
        for (XdmItem item : xpc.evaluate("//svrl:failed-assert", doc)) {
            XdmNode node     = (XdmNode) item;
            String patternId = node.attribute("patternId");
            String test      = node.attribute("test");
            String location  = simplifyLocation(node.attribute("location"));
            XdmItem textItem = xpc.evaluateSingle("normalize-space(svrl:text)", node);
            String message   = textItem != null ? textItem.getStringValue() : "";
            result.add(new FailedAssertion(patternId, test, location, message));
        }
        return result;
    }

    // /Q{}country-chap[1]/Q{}chaphead[1]/Q{}year[1]  →  /country-chap/chaphead/year
    private String simplifyLocation(String loc) {
        if (loc == null) return "";
        return loc.replaceAll("Q\\{[^}]*\\}", "").replaceAll("\\[\\d+\\]", "");
    }
}
