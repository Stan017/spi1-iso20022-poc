package com.stan.poc.spi2iso.jaxb;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.StringWriter;

/**
 * Utilidad de marshalling XML para los documentos ISO 20022 de este PoC.
 */
public final class XmlUtil {

    private XmlUtil() {
    }

    public static String marshal(Object jaxbObject) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(jaxbObject.getClass());
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            marshaller.marshal(jaxbObject, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Error serializando a XML: " + e.getMessage(), e);
        }
    }
}
