package com.stan.poc.spi2iso.jaxb;

import com.stan.poc.spi2iso.jaxb.adapter.LocalDateAdapter;
import com.stan.poc.spi2iso.jaxb.adapter.OffsetDateTimeAdapter;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo JAXB del mensaje ISO 20022 pacs.008.001.08 (FIToFICustomerCreditTransfer).
 *
 * IMPORTANTE - limite de este PoC: este archivo NO fue generado a partir del XSD oficial
 * descargado de iso20022.org. El entorno de trabajo usado para este proyecto no tiene
 * salida de red hacia ese dominio (solo repositorios de paquetes y GitHub), por lo que
 * estas clases fueron escritas a mano reproduciendo la jerarquia y los nombres de
 * elementos reales del mensaje pacs.008.001.08 (segun la documentacion publica de ISO
 * 20022 y, para el detalle de campos especificos de pagos domesticos, por analogia con
 * el mecanismo MOL/Bre-B de Colombia - ver README.md). Antes de usar esto en un entorno
 * real, se debe regenerar este binding con un generador JAXB (xjc) a partir del XSD
 * oficial de ISO 20022.
 */
@XmlRootElement(name = "Document", namespace = Pacs008Document.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class Pacs008Document {

    public static final String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

    @XmlElement(name = "FIToFICstmrCdtTrf", namespace = NS)
    private FiToFiCustomerCreditTransfer fiToFiCstmrCdtTrf;

    public Pacs008Document() {
    }

    public Pacs008Document(FiToFiCustomerCreditTransfer body) {
        this.fiToFiCstmrCdtTrf = body;
    }

    public FiToFiCustomerCreditTransfer getFiToFiCstmrCdtTrf() {
        return fiToFiCstmrCdtTrf;
    }

    public void setFiToFiCstmrCdtTrf(FiToFiCustomerCreditTransfer fiToFiCstmrCdtTrf) {
        this.fiToFiCstmrCdtTrf = fiToFiCstmrCdtTrf;
    }

    // ---------------------------------------------------------------------

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FiToFiCustomerCreditTransfer {

        @XmlElement(name = "GrpHdr", namespace = NS)
        private GroupHeader grpHdr;

        @XmlElement(name = "CdtTrfTxInf", namespace = NS)
        private List<CreditTransferTransactionInformation> cdtTrfTxInf = new ArrayList<>();

        public GroupHeader getGrpHdr() {
            return grpHdr;
        }

        public void setGrpHdr(GroupHeader grpHdr) {
            this.grpHdr = grpHdr;
        }

        public List<CreditTransferTransactionInformation> getCdtTrfTxInf() {
            return cdtTrfTxInf;
        }

        public void setCdtTrfTxInf(List<CreditTransferTransactionInformation> cdtTrfTxInf) {
            this.cdtTrfTxInf = cdtTrfTxInf;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GroupHeader {

        @XmlElement(name = "MsgId", namespace = NS)
        private String msgId;

        @XmlElement(name = "CreDtTm", namespace = NS)
        @XmlJavaTypeAdapter(OffsetDateTimeAdapter.class)
        private OffsetDateTime creDtTm;

        @XmlElement(name = "NbOfTxs", namespace = NS)
        private String nbOfTxs;

        @XmlElement(name = "SttlmInf", namespace = NS)
        private SettlementInformation sttlmInf;

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

        public String getNbOfTxs() {
            return nbOfTxs;
        }

        public void setNbOfTxs(String nbOfTxs) {
            this.nbOfTxs = nbOfTxs;
        }

        public SettlementInformation getSttlmInf() {
            return sttlmInf;
        }

        public void setSttlmInf(SettlementInformation sttlmInf) {
            this.sttlmInf = sttlmInf;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SettlementInformation {
        @XmlElement(name = "SttlmMtd", namespace = NS)
        private String sttlmMtd; // ej. "CLRG" (clearing)

        public String getSttlmMtd() {
            return sttlmMtd;
        }

        public void setSttlmMtd(String sttlmMtd) {
            this.sttlmMtd = sttlmMtd;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CreditTransferTransactionInformation {

        @XmlElement(name = "PmtId", namespace = NS)
        private PaymentIdentification pmtId;

        @XmlElement(name = "IntrBkSttlmAmt", namespace = NS)
        private ActiveCurrencyAndAmount intrBkSttlmAmt;

        @XmlElement(name = "IntrBkSttlmDt", namespace = NS)
        @XmlJavaTypeAdapter(LocalDateAdapter.class)
        private LocalDate intrBkSttlmDt;

        @XmlElement(name = "InstgAgt", namespace = NS)
        private BranchAndFinancialInstitutionIdentification instgAgt;

        @XmlElement(name = "InstdAgt", namespace = NS)
        private BranchAndFinancialInstitutionIdentification instdAgt;

        @XmlElement(name = "Dbtr", namespace = NS)
        private PartyIdentification dbtr;

        @XmlElement(name = "DbtrAcct", namespace = NS)
        private CashAccount dbtrAcct;

        @XmlElement(name = "DbtrAgt", namespace = NS)
        private BranchAndFinancialInstitutionIdentification dbtrAgt;

        @XmlElement(name = "CdtrAgt", namespace = NS)
        private BranchAndFinancialInstitutionIdentification cdtrAgt;

        @XmlElement(name = "Cdtr", namespace = NS)
        private PartyIdentification cdtr;

        @XmlElement(name = "CdtrAcct", namespace = NS)
        private CashAccount cdtrAcct;

        @XmlElement(name = "RmtInf", namespace = NS)
        private RemittanceInformation rmtInf;

        // getters / setters

        public PaymentIdentification getPmtId() {
            return pmtId;
        }

        public void setPmtId(PaymentIdentification pmtId) {
            this.pmtId = pmtId;
        }

        public ActiveCurrencyAndAmount getIntrBkSttlmAmt() {
            return intrBkSttlmAmt;
        }

        public void setIntrBkSttlmAmt(ActiveCurrencyAndAmount intrBkSttlmAmt) {
            this.intrBkSttlmAmt = intrBkSttlmAmt;
        }

        public LocalDate getIntrBkSttlmDt() {
            return intrBkSttlmDt;
        }

        public void setIntrBkSttlmDt(LocalDate intrBkSttlmDt) {
            this.intrBkSttlmDt = intrBkSttlmDt;
        }

        public BranchAndFinancialInstitutionIdentification getInstgAgt() {
            return instgAgt;
        }

        public void setInstgAgt(BranchAndFinancialInstitutionIdentification instgAgt) {
            this.instgAgt = instgAgt;
        }

        public BranchAndFinancialInstitutionIdentification getInstdAgt() {
            return instdAgt;
        }

        public void setInstdAgt(BranchAndFinancialInstitutionIdentification instdAgt) {
            this.instdAgt = instdAgt;
        }

        public PartyIdentification getDbtr() {
            return dbtr;
        }

        public void setDbtr(PartyIdentification dbtr) {
            this.dbtr = dbtr;
        }

        public CashAccount getDbtrAcct() {
            return dbtrAcct;
        }

        public void setDbtrAcct(CashAccount dbtrAcct) {
            this.dbtrAcct = dbtrAcct;
        }

        public BranchAndFinancialInstitutionIdentification getDbtrAgt() {
            return dbtrAgt;
        }

        public void setDbtrAgt(BranchAndFinancialInstitutionIdentification dbtrAgt) {
            this.dbtrAgt = dbtrAgt;
        }

        public BranchAndFinancialInstitutionIdentification getCdtrAgt() {
            return cdtrAgt;
        }

        public void setCdtrAgt(BranchAndFinancialInstitutionIdentification cdtrAgt) {
            this.cdtrAgt = cdtrAgt;
        }

        public PartyIdentification getCdtr() {
            return cdtr;
        }

        public void setCdtr(PartyIdentification cdtr) {
            this.cdtr = cdtr;
        }

        public CashAccount getCdtrAcct() {
            return cdtrAcct;
        }

        public void setCdtrAcct(CashAccount cdtrAcct) {
            this.cdtrAcct = cdtrAcct;
        }

        public RemittanceInformation getRmtInf() {
            return rmtInf;
        }

        public void setRmtInf(RemittanceInformation rmtInf) {
            this.rmtInf = rmtInf;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PaymentIdentification {
        @XmlElement(name = "InstrId", namespace = NS)
        private String instrId;

        @XmlElement(name = "EndToEndId", namespace = NS)
        private String endToEndId;

        public String getInstrId() {
            return instrId;
        }

        public void setInstrId(String instrId) {
            this.instrId = instrId;
        }

        public String getEndToEndId() {
            return endToEndId;
        }

        public void setEndToEndId(String endToEndId) {
            this.endToEndId = endToEndId;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ActiveCurrencyAndAmount {

        @XmlAttribute(name = "Ccy")
        private String ccy;

        @XmlValue
        private BigDecimal value;

        public ActiveCurrencyAndAmount() {
        }

        public ActiveCurrencyAndAmount(BigDecimal value, String ccy) {
            this.value = value;
            this.ccy = ccy;
        }

        public String getCcy() {
            return ccy;
        }

        public void setCcy(String ccy) {
            this.ccy = ccy;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BranchAndFinancialInstitutionIdentification {
        @XmlElement(name = "FinInstnId", namespace = NS)
        private FinancialInstitutionIdentification finInstnId;

        public FinancialInstitutionIdentification getFinInstnId() {
            return finInstnId;
        }

        public void setFinInstnId(FinancialInstitutionIdentification finInstnId) {
            this.finInstnId = finInstnId;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FinancialInstitutionIdentification {
        @XmlElement(name = "BICFI", namespace = NS)
        private String bicfi;

        @XmlElement(name = "ClrSysMmbId", namespace = NS)
        private ClearingSystemMemberIdentification clrSysMmbId;

        @XmlElement(name = "Nm", namespace = NS)
        private String nm;

        public String getBicfi() {
            return bicfi;
        }

        public void setBicfi(String bicfi) {
            this.bicfi = bicfi;
        }

        public ClearingSystemMemberIdentification getClrSysMmbId() {
            return clrSysMmbId;
        }

        public void setClrSysMmbId(ClearingSystemMemberIdentification clrSysMmbId) {
            this.clrSysMmbId = clrSysMmbId;
        }

        public String getNm() {
            return nm;
        }

        public void setNm(String nm) {
            this.nm = nm;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ClearingSystemMemberIdentification {
        @XmlElement(name = "MmbId", namespace = NS)
        private String mmbId;

        public String getMmbId() {
            return mmbId;
        }

        public void setMmbId(String mmbId) {
            this.mmbId = mmbId;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PartyIdentification {
        @XmlElement(name = "Nm", namespace = NS)
        private String nm;

        @XmlElement(name = "Id", namespace = NS)
        private PartyIdentificationChoice id;

        public String getNm() {
            return nm;
        }

        public void setNm(String nm) {
            this.nm = nm;
        }

        public PartyIdentificationChoice getId() {
            return id;
        }

        public void setId(PartyIdentificationChoice id) {
            this.id = id;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PartyIdentificationChoice {
        @XmlElement(name = "PrvtId", namespace = NS)
        private PersonIdentification prvtId;

        public PersonIdentification getPrvtId() {
            return prvtId;
        }

        public void setPrvtId(PersonIdentification prvtId) {
            this.prvtId = prvtId;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PersonIdentification {
        @XmlElement(name = "Othr", namespace = NS)
        private GenericPersonIdentification othr;

        public GenericPersonIdentification getOthr() {
            return othr;
        }

        public void setOthr(GenericPersonIdentification othr) {
            this.othr = othr;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GenericPersonIdentification {
        @XmlElement(name = "Id", namespace = NS)
        private String id;

        @XmlElement(name = "SchmeNm", namespace = NS)
        private SchemeName schmeNm;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public SchemeName getSchmeNm() {
            return schmeNm;
        }

        public void setSchmeNm(SchemeName schmeNm) {
            this.schmeNm = schmeNm;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SchemeName {
        /** Codigo de tipo de identificacion: CI, CE, RUC o PASS (ver README). */
        @XmlElement(name = "Prtry", namespace = NS)
        private String prtry;

        public String getPrtry() {
            return prtry;
        }

        public void setPrtry(String prtry) {
            this.prtry = prtry;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CashAccount {
        @XmlElement(name = "Id", namespace = NS)
        private CashAccountIdentification id;

        public CashAccountIdentification getId() {
            return id;
        }

        public void setId(CashAccountIdentification id) {
            this.id = id;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CashAccountIdentification {
        @XmlElement(name = "Othr", namespace = NS)
        private GenericAccountIdentification othr;

        public GenericAccountIdentification getOthr() {
            return othr;
        }

        public void setOthr(GenericAccountIdentification othr) {
            this.othr = othr;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GenericAccountIdentification {
        @XmlElement(name = "Id", namespace = NS)
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RemittanceInformation {
        @XmlElement(name = "Ustrd", namespace = NS)
        private String ustrd;

        public String getUstrd() {
            return ustrd;
        }

        public void setUstrd(String ustrd) {
            this.ustrd = ustrd;
        }
    }
}
