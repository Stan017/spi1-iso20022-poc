package com.stan.poc.spi2iso.jaxb;

import com.stan.poc.spi2iso.jaxb.adapter.OffsetDateTimeAdapter;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo JAXB del mensaje ISO 20022 pacs.002.001.10 (FIToFIPaymentStatusReport).
 * Mismo limite y alcance descrito en Pacs008Document: binding escrito a mano,
 * no generado desde el XSD oficial de iso20022.org. Ver README.md.
 */
@XmlRootElement(name = "Document", namespace = Pacs002Document.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class Pacs002Document {

    public static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.002.001.10";

    @XmlElement(name = "FIToFIPmtStsRpt", namespace = NS)
    private FiToFiPaymentStatusReport fiToFiPmtStsRpt;

    public Pacs002Document() {
    }

    public Pacs002Document(FiToFiPaymentStatusReport body) {
        this.fiToFiPmtStsRpt = body;
    }

    public FiToFiPaymentStatusReport getFiToFiPmtStsRpt() {
        return fiToFiPmtStsRpt;
    }

    public void setFiToFiPmtStsRpt(FiToFiPaymentStatusReport fiToFiPmtStsRpt) {
        this.fiToFiPmtStsRpt = fiToFiPmtStsRpt;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FiToFiPaymentStatusReport {

        @XmlElement(name = "GrpHdr", namespace = NS)
        private GroupHeader grpHdr;

        @XmlElement(name = "TxInfAndSts", namespace = NS)
        private List<TransactionInformationAndStatus> txInfAndSts = new ArrayList<>();

        public GroupHeader getGrpHdr() {
            return grpHdr;
        }

        public void setGrpHdr(GroupHeader grpHdr) {
            this.grpHdr = grpHdr;
        }

        public List<TransactionInformationAndStatus> getTxInfAndSts() {
            return txInfAndSts;
        }

        public void setTxInfAndSts(List<TransactionInformationAndStatus> txInfAndSts) {
            this.txInfAndSts = txInfAndSts;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GroupHeader {
        @XmlElement(name = "MsgId", namespace = NS)
        private String msgId;

        @XmlElement(name = "CreDtTm", namespace = NS)
        @XmlJavaTypeAdapter(OffsetDateTimeAdapter.class)
        private OffsetDateTime creDtTm;

        public String getMsgId() {
            return msgId;
        }

        public void setMsgId(String msgId) {
            this.msgId = msgId;
        }

        public OffsetDateTime getCreDtTm() {
            return creDtTm;
        }

        public void setCreDtTm(OffsetDateTime creDtTm) {
            this.creDtTm = creDtTm;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TransactionInformationAndStatus {

        @XmlElement(name = "OrgnlInstrId", namespace = NS)
        private String orgnlInstrId;

        @XmlElement(name = "OrgnlEndToEndId", namespace = NS)
        private String orgnlEndToEndId;

        /** ACTC = aceptado tecnicamente, ACSC = liquidado, RJCT = rechazado. */
        @XmlElement(name = "TxSts", namespace = NS)
        private String txSts;

        @XmlElement(name = "StsRsnInf", namespace = NS)
        private StatusReasonInformation stsRsnInf;

        public String getOrgnlInstrId() {
            return orgnlInstrId;
        }

        public void setOrgnlInstrId(String orgnlInstrId) {
            this.orgnlInstrId = orgnlInstrId;
        }

        public String getOrgnlEndToEndId() {
            return orgnlEndToEndId;
        }

        public void setOrgnlEndToEndId(String orgnlEndToEndId) {
            this.orgnlEndToEndId = orgnlEndToEndId;
        }

        public String getTxSts() {
            return txSts;
        }

        public void setTxSts(String txSts) {
            this.txSts = txSts;
        }

        public StatusReasonInformation getStsRsnInf() {
            return stsRsnInf;
        }

        public void setStsRsnInf(StatusReasonInformation stsRsnInf) {
            this.stsRsnInf = stsRsnInf;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class StatusReasonInformation {
        @XmlElement(name = "Rsn", namespace = NS)
        private ReasonChoice rsn;

        @XmlElement(name = "AddtlInf", namespace = NS)
        private String addtlInf;

        public ReasonChoice getRsn() {
            return rsn;
        }

        public void setRsn(ReasonChoice rsn) {
            this.rsn = rsn;
        }

        public String getAddtlInf() {
            return addtlInf;
        }

        public void setAddtlInf(String addtlInf) {
            this.addtlInf = addtlInf;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ReasonChoice {
        @XmlElement(name = "Cd", namespace = NS)
        private String cd; // ej. AC04 (cuenta cerrada), AM02 (monto no permitido), AG01, etc.

        public String getCd() {
            return cd;
        }

        public void setCd(String cd) {
            this.cd = cd;
        }
    }
}
