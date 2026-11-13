package com.stan.poc.spi2iso.gateway;

import com.stan.poc.spi2iso.jaxb.Pacs002Document;
import com.stan.poc.spi2iso.jaxb.Pacs008Document;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Simula el comportamiento del switch/conmutador del BCE (SPI / futura RPI) frente a un
 * mensaje pacs.008 entrante, devolviendo un pacs.002 (Payment Status Report).
 *
 * Esto es un SIMULADOR local para la demo: no se conecta a ningun sistema real del
 * Banco Central del Ecuador. El comportamiento (aceptar, rechazar o no responder) se
 * decide deterministicamente a partir del contenido del mensaje para que la demo sea
 * reproducible:
 *   - Si el monto es exactamente 999.99          -> RJCT (rechazado, motivo AM02)
 *   - Si la referencia/Ustrd contiene "TIMEOUT"  -> no responde (se simula un timeout)
 *   - En cualquier otro caso                      -> ACSC (liquidado)
 */
@Component
public class SwitchGatewaySimulator {

    public static final String RESULT_TIMEOUT = "TIMEOUT";

    public SimulatedResponse procesar(Pacs008Document pacs008) {
        var tx = pacs008.getFiToFiCstmrCdtTrf().getCdtTrfTxInf().get(0);

        String ustrd = tx.getRmtInf() != null ? tx.getRmtInf().getUstrd() : null;
        if (ustrd != null && ustrd.toUpperCase().contains("TIMEOUT")) {
            return new SimulatedResponse(RESULT_TIMEOUT, null);
        }

        boolean forzarRechazo = tx.getIntrBkSttlmAmt() != null
                && tx.getIntrBkSttlmAmt().getValue() != null
                && tx.getIntrBkSttlmAmt().getValue().doubleValue() == 999.99d;

        Pacs002Document.TransactionInformationAndStatus estado =
                new Pacs002Document.TransactionInformationAndStatus();
        estado.setOrgnlInstrId(tx.getPmtId().getInstrId());
        estado.setOrgnlEndToEndId(tx.getPmtId().getEndToEndId());

        if (forzarRechazo) {
            estado.setTxSts("RJCT");
            var rsnInf = new Pacs002Document.StatusReasonInformation();
            var rsn = new Pacs002Document.ReasonChoice();
            rsn.setCd("AM02"); // "NotAllowedAmount" - codigo ISO 20022 estandar de motivo de rechazo
            rsnInf.setRsn(rsn);
            rsnInf.setAddtlInf("Monto no permitido (regla de demo del simulador de switch)");
            estado.setStsRsnInf(rsnInf);
        } else {
            estado.setTxSts("ACSC");
        }

        var grpHdr = new Pacs002Document.GroupHeader();
        grpHdr.setMsgId("RESP-" + UUID.randomUUID());
        grpHdr.setCreDtTm(OffsetDateTime.now(ZoneOffset.UTC));

        var body = new Pacs002Document.FiToFiPaymentStatusReport();
        body.setGrpHdr(grpHdr);
        body.getTxInfAndSts().add(estado);

        Pacs002Document doc = new Pacs002Document(body);
        return new SimulatedResponse(estado.getTxSts(), doc);
    }

    public record SimulatedResponse(String estado, Pacs002Document pacs002) {
    }
}
