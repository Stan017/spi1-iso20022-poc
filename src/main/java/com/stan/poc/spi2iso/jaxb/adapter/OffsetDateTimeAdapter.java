package com.stan.poc.spi2iso.jaxb.adapter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JAXB no serializa java.time.OffsetDateTime de forma nativa sin un adaptador explicito
 * (a diferencia de java.util.Date/Calendar, que si tienen soporte incorporado). Este
 * adaptador lo serializa/deserializa como xsd:dateTime (ISO-8601 con offset), que es el
 * tipo correcto para elementos como GrpHdr/CreDtTm en los mensajes ISO 20022.
 */
public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

    @Override
    public OffsetDateTime unmarshal(String v) {
        return v == null ? null : OffsetDateTime.parse(v, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public String marshal(OffsetDateTime v) {
        return v == null ? null : v.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
