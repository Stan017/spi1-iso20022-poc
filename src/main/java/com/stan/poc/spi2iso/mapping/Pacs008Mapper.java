package com.stan.poc.spi2iso.mapping;

import com.stan.poc.spi2iso.jaxb.XmlUtil;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.AccountIdentification4Choice;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.ActiveCurrencyAndAmount;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.BranchAndFinancialInstitutionIdentification8;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.CashAccount40;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.ChargeBearerType1Code;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.ClearingSystemMemberIdentification2;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.CreditTransferTransaction73;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.Document;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.FIToFICustomerCreditTransferV14;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.FinancialInstitutionIdentification23;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.GenericAccountIdentification1;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.GenericPersonIdentification2;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.GroupHeader131;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.Party52Choice;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.PartyIdentification272;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.PaymentIdentification13;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.PersonIdentification18;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.PersonIdentificationSchemeName1Choice;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.RemittanceInformation26;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.SettlementInstruction15;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.SettlementMethod1Code;
import com.stan.poc.spi2iso.model.Spi1Record;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Mapea un Spi1Record (formato SPI1 legado) a un Document pacs.008.001.14 ISO 20022,
 * generado por xjc a partir del XSD OFICIAL de iso20022.org (ver
 * src/main/resources/xsd/pacs008/ y README.md, seccion "Alcance, fuentes y supuestos").
 *
 * Mapeo principal (misma intencion de diseno de la version anterior escrita a mano,
 * ahora contra los nombres de tipo reales del esquema oficial):
 *   institucionOrdenante(Codigo/Nombre) -> InstgAgt/DbtrAgt/FinInstnId (ClrSysMmbId/MmbId, Nm)
 *   institucionReceptora(Codigo/Nombre) -> InstdAgt/CdtrAgt/FinInstnId (ClrSysMmbId/MmbId, Nm)
 *   cuentaOrigen                        -> DbtrAcct/Id/Othr/Id
 *   cuentaDestino                       -> CdtrAcct/Id/Othr/Id
 *   monto + moneda                      -> IntrBkSttlmAmt (value, Ccy)
 *   tipoIdOrdenante + idOrdenante       -> Dbtr/Id/PrvtId/Othr/Id + SchmeNm/Prtry
 *   nombreOrdenante                     -> Dbtr/Nm
 *   tipoIdBeneficiario + idBeneficiario -> Cdtr/Id/PrvtId/Othr/Id + SchmeNm/Prtry
 *   nombreBeneficiario                  -> Cdtr/Nm
 *   numeroInstruccion                   -> PmtId/InstrId
 *   referencia                          -> PmtId/EndToEndId (fallback a numeroInstruccion) y RmtInf/Ustrd
 *   fechaHora                           -> IntrBkSttlmDt (fecha) y GrpHdr/CreDtTm del mensaje contenedor
 *
 * El SPI1 de este PoC no modela dos campos que el esquema real exige siempre:
 * ChrgBr (reparto de comisiones) y SttlmMtd (metodo de liquidacion), ni un catalogo
 * oficial para el tipo de identificacion (Dbtr/Cdtr SchmeNm/Prtry). Estos tres son
 * supuestos razonados, no una especificacion del BCE (ver README, "Alcance, fuentes
 * y supuestos" #3 y #4) - por eso NO estan hardcodeados aqui: vienen de
 * {@link Pacs008MappingProperties} (poc.mapping.pacs008.* en application.properties),
 * para que ajustarlos cuando el BCE publique su especificacion sea un cambio de
 * configuracion, no una reescritura de este mapper.
 */
@Component
public class Pacs008Mapper {

    private final Pacs008MappingProperties mappingConfig;

    public Pacs008Mapper(Pacs008MappingProperties mappingConfig) {
        this.mappingConfig = mappingConfig;
    }

    public Document toPacs008(Spi1Record r) {
        var tx = new CreditTransferTransaction73();

        var pmtId = new PaymentIdentification13();
        pmtId.setInstrId(r.getNumeroInstruccion());
        pmtId.setEndToEndId(blankSafe(r.getReferencia(), r.getNumeroInstruccion()));
        tx.setPmtId(pmtId);

        var amount = new ActiveCurrencyAndAmount();
        amount.setValue(r.getMonto());
        amount.setCcy(blankSafe(r.getMoneda(), "USD"));
        tx.setIntrBkSttlmAmt(amount);

        if (r.getFechaHora() != null) {
            tx.setIntrBkSttlmDt(XmlUtil.toXmlDate(r.getFechaHora().toLocalDate()));
        }

        tx.setChrgBr(ChargeBearerType1Code.valueOf(mappingConfig.getChargeBearer()));

        tx.setInstgAgt(agente(r.getInstitucionOrdenanteCodigo(), r.getInstitucionOrdenanteNombre()));
        tx.setInstdAgt(agente(r.getInstitucionReceptoraCodigo(), r.getInstitucionReceptoraNombre()));
        tx.setDbtrAgt(agente(r.getInstitucionOrdenanteCodigo(), r.getInstitucionOrdenanteNombre()));
        tx.setCdtrAgt(agente(r.getInstitucionReceptoraCodigo(), r.getInstitucionReceptoraNombre()));

        tx.setDbtr(parte(r.getNombreOrdenante(), r.getTipoIdOrdenante(), r.getIdOrdenante()));
        tx.setDbtrAcct(cuenta(r.getCuentaOrigen()));

        tx.setCdtr(parte(r.getNombreBeneficiario(), r.getTipoIdBeneficiario(), r.getIdBeneficiario()));
        tx.setCdtrAcct(cuenta(r.getCuentaDestino()));

        if (r.getReferencia() != null && !r.getReferencia().isBlank()) {
            var rmt = new RemittanceInformation26();
            rmt.getUstrd().add(r.getReferencia());
            tx.setRmtInf(rmt);
        }

        var grpHdr = new GroupHeader131();
        grpHdr.setMsgId("MSG-" + UUID.randomUUID());
        grpHdr.setCreDtTm(XmlUtil.toXmlDateTime(OffsetDateTime.now(ZoneOffset.UTC)));
        grpHdr.setNbOfTxs("1");
        var sttlm = new SettlementInstruction15();
        sttlm.setSttlmMtd(SettlementMethod1Code.valueOf(mappingConfig.getSettlementMethod()));
        grpHdr.setSttlmInf(sttlm);

        var body = new FIToFICustomerCreditTransferV14();
        body.setGrpHdr(grpHdr);
        body.getCdtTrfTxInf().add(tx);

        var document = new Document();
        document.setFIToFICstmrCdtTrf(body);
        return document;
    }

    private BranchAndFinancialInstitutionIdentification8 agente(String codigo, String nombre) {
        var fin = new FinancialInstitutionIdentification23();
        if (codigo != null && !codigo.isBlank()) {
            var clrSys = new ClearingSystemMemberIdentification2();
            clrSys.setMmbId(codigo);
            fin.setClrSysMmbId(clrSys);
        }
        fin.setNm(nombre);
        var agt = new BranchAndFinancialInstitutionIdentification8();
        agt.setFinInstnId(fin);
        return agt;
    }

    private PartyIdentification272 parte(String nombre, String tipoId, String id) {
        var party = new PartyIdentification272();
        party.setNm(nombre);

        if (id != null && !id.isBlank()) {
            var schemeName = new PersonIdentificationSchemeName1Choice();
            schemeName.setPrtry(mappingConfig.translateIdentificationType(tipoId)); // CI | CE | RUC | PASS -> poc.mapping.pacs008.identification-types

            var generic = new GenericPersonIdentification2();
            generic.setId(id);
            generic.setSchmeNm(schemeName);

            var person = new PersonIdentification18();
            person.getOthr().add(generic);

            var choice = new Party52Choice();
            choice.setPrvtId(person);

            party.setId(choice);
        }
        return party;
    }

    private CashAccount40 cuenta(String numeroCuenta) {
        var generic = new GenericAccountIdentification1();
        generic.setId(numeroCuenta);
        var id = new AccountIdentification4Choice();
        id.setOthr(generic);
        var acct = new CashAccount40();
        acct.setId(id);
        return acct;
    }

    private String blankSafe(String preferido, String fallback) {
        return (preferido == null || preferido.isBlank()) ? fallback : preferido;
    }
}
