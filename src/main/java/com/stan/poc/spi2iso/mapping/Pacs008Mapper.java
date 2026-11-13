package com.stan.poc.spi2iso.mapping;

import com.stan.poc.spi2iso.jaxb.Pacs008Document;
import com.stan.poc.spi2iso.model.Spi1Record;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Mapea un Spi1Record (formato SPI1 legado) a un Pacs008Document (ISO 20022
 * pacs.008.001.08), siguiendo la tabla de mapeo de campos documentada en la
 * Capa 2 (Datos) del documento de diseno del PoC.
 *
 * Mapeo principal:
 *   institucionOrdenante(Codigo/Nombre) -> InstgAgt/FinInstnId (ClrSysMmbId/MmbId, Nm)
 *   institucionReceptora(Codigo/Nombre) -> InstdAgt/FinInstnId (ClrSysMmbId/MmbId, Nm)
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
 */
@Component
public class Pacs008Mapper {

    public Pacs008Document toPacs008(Spi1Record r) {
        var tx = new Pacs008Document.CreditTransferTransactionInformation();

        var pmtId = new Pacs008Document.PaymentIdentification();
        pmtId.setInstrId(r.getNumeroInstruccion());
        pmtId.setEndToEndId(blankSafe(r.getReferencia(), r.getNumeroInstruccion()));
        tx.setPmtId(pmtId);

        tx.setIntrBkSttlmAmt(new Pacs008Document.ActiveCurrencyAndAmount(
                r.getMonto(), blankSafe(r.getMoneda(), "USD")));

        if (r.getFechaHora() != null) {
            tx.setIntrBkSttlmDt(r.getFechaHora().toLocalDate());
        }

        tx.setInstgAgt(agente(r.getInstitucionOrdenanteCodigo(), r.getInstitucionOrdenanteNombre()));
        tx.setInstdAgt(agente(r.getInstitucionReceptoraCodigo(), r.getInstitucionReceptoraNombre()));
        tx.setDbtrAgt(agente(r.getInstitucionOrdenanteCodigo(), r.getInstitucionOrdenanteNombre()));
        tx.setCdtrAgt(agente(r.getInstitucionReceptoraCodigo(), r.getInstitucionReceptoraNombre()));

        tx.setDbtr(parte(r.getNombreOrdenante(), r.getTipoIdOrdenante(), r.getIdOrdenante()));
        tx.setDbtrAcct(cuenta(r.getCuentaOrigen()));

        tx.setCdtr(parte(r.getNombreBeneficiario(), r.getTipoIdBeneficiario(), r.getIdBeneficiario()));
        tx.setCdtrAcct(cuenta(r.getCuentaDestino()));

        if (r.getReferencia() != null && !r.getReferencia().isBlank()) {
            var rmt = new Pacs008Document.RemittanceInformation();
            rmt.setUstrd(r.getReferencia());
            tx.setRmtInf(rmt);
        }

        var grpHdr = new Pacs008Document.GroupHeader();
        grpHdr.setMsgId("MSG-" + UUID.randomUUID());
        grpHdr.setCreDtTm(OffsetDateTime.now(ZoneOffset.UTC));
        grpHdr.setNbOfTxs("1");
        var sttlm = new Pacs008Document.SettlementInformation();
        sttlm.setSttlmMtd("CLRG");
        grpHdr.setSttlmInf(sttlm);

        var body = new Pacs008Document.FiToFiCustomerCreditTransfer();
        body.setGrpHdr(grpHdr);
        body.getCdtTrfTxInf().add(tx);

        return new Pacs008Document(body);
    }

    private Pacs008Document.BranchAndFinancialInstitutionIdentification agente(String codigo, String nombre) {
        var fin = new Pacs008Document.FinancialInstitutionIdentification();
        if (codigo != null && !codigo.isBlank()) {
            var clrSys = new Pacs008Document.ClearingSystemMemberIdentification();
            clrSys.setMmbId(codigo);
            fin.setClrSysMmbId(clrSys);
        }
        fin.setNm(nombre);
        var agt = new Pacs008Document.BranchAndFinancialInstitutionIdentification();
        agt.setFinInstnId(fin);
        return agt;
    }

    private Pacs008Document.PartyIdentification parte(String nombre, String tipoId, String id) {
        var party = new Pacs008Document.PartyIdentification();
        party.setNm(nombre);

        if (id != null && !id.isBlank()) {
            var schemeName = new Pacs008Document.SchemeName();
            schemeName.setPrtry(tipoId); // CI | CE | RUC | PASS

            var generic = new Pacs008Document.GenericPersonIdentification();
            generic.setId(id);
            generic.setSchmeNm(schemeName);

            var person = new Pacs008Document.PersonIdentification();
            person.setOthr(generic);

            var choice = new Pacs008Document.PartyIdentificationChoice();
            choice.setPrvtId(person);

            party.setId(choice);
        }
        return party;
    }

    private Pacs008Document.CashAccount cuenta(String numeroCuenta) {
        var generic = new Pacs008Document.GenericAccountIdentification();
        generic.setId(numeroCuenta);
        var id = new Pacs008Document.CashAccountIdentification();
        id.setOthr(generic);
        var acct = new Pacs008Document.CashAccount();
        acct.setId(id);
        return acct;
    }

    private String blankSafe(String preferido, String fallback) {
        return (preferido == null || preferido.isBlank()) ? fallback : preferido;
    }
}
