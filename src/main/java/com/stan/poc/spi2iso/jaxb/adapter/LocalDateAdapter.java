package com.stan.poc.spi2iso.jaxb.adapter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Serializa java.time.LocalDate como xsd:date (yyyy-MM-dd), usado por ejemplo en
 * CdtTrfTxInf/IntrBkSttlmDt. Ver nota en OffsetDateTimeAdapter.
 */
public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {

    @Override
    public LocalDate unmarshal(String v) {
        return v == null ? null : LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public String marshal(LocalDate v) {
        return v == null ? null : v.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
