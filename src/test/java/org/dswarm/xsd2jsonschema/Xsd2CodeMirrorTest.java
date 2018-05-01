package org.dswarm.xsd2jsonschema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formulasearchengine.mathmltools.mml.MathDoc;
import org.dswarm.xsd2jsonschema.model.JSRoot;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import static org.junit.Assert.*;

public class Xsd2CodeMirrorTest {

    @Test
    public void parse() throws SAXException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        final Xsd2CodeMirror schemaParser = new Xsd2CodeMirror();
        schemaParser.parse(MathDoc.getMathMLSchema());
        schemaParser.apply();
    }

}
