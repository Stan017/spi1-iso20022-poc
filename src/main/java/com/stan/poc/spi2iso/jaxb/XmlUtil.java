package com.stan.poc.spi2iso.jaxb;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.GregorianCalendar;

/**
 * Utilidad de marshalling XML y de conversion fecha/hora para los mensajes ISO 20022
 * generados por xjc (com.stan.poc.spi2iso.jaxb.generated.{pacs008,pacs002}) a partir
 * del XSD oficial de iso20022.org. Ver README.md, seccion "Alcance, fuentes y supuestos".
 *
 * El elemento raiz "Document" de ambos esquemas se declara como
 * {@code <xs:element name="Document" type="Document"/>} - un elemento separado del
 * complexType, no {@code @XmlRootElement} sobre la clase Document. Por eso xjc genera
 * la fabrica {@code ObjectFactory.createDocument(...)}, que envuelve el POJO en un
 * {@link JAXBElement}; el marshalling tiene que pasar por ese JAXBElement, no por el
 * POJO Document directamente (si se intenta, JAXB lanza
 * "unable to marshal... missing @XmlRootElement").
 */
public final class XmlUtil {

    private static final DatatypeFactory DATATYPE_FACTORY = createDatatypeFactory();

    private XmlUtil() {
    }

    public static String marshalPacs008(com.stan.poc.spi2iso.jaxb.generated.pacs008.Document document) {
        var factory = new com.stan.poc.spi2iso.jaxb.generated.pacs008.ObjectFactory();
        return marshal(factory.createDocument(document));
    }

    public static String marshalPacs002(com.stan.poc.spi2iso.jaxb.generated.pacs002.Document document) {
        var factory = new com.stan.poc.spi2iso.jaxb.generated.pacs002.ObjectFactory();
        return marshal(factory.createDocument(document));
    }

    /** Convierte un LocalDate a xsd:date (sin hora ni huso horario) - ej. IntrBkSttlmDt. */
    public static XMLGregorianCalendar toXmlDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return DATATYPE_FACTORY.newXMLGregorianCalendarDate(
                date.getYear(), date.getMonthValue(), date.getDayOfMonth(), DatatypeConstants.FIELD_UNDEFINED);
    }

    /** Convierte un OffsetDateTime a xsd:dateTime (con offset) - ej. GrpHdr/CreDtTm. */
    public static XMLGregorianCalendar toXmlDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return DATATYPE_FACTORY.newXMLGregorianCalendar(GregorianCalendar.from(dateTime.toZonedDateTime()));
    }

    private static String marshal(JAXBElement<?> element) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(element.getDeclaredType());
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter sw = new StringWriter();
            marshaller.marshal(element, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Error serializando a XML: " + e.getMessage(), e);
        }
    }

    private static DatatypeFactory createDatatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("No se pudo inicializar DatatypeFactory: " + e.getMessage(), e);
        }
    }
}
