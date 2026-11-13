package com.stan.poc.spi2iso.gateway;

import com.stan.poc.spi2iso.jaxb.XmlUtil;
import com.stan.poc.spi2iso.jaxb.generated.pacs002.FIToFIPaymentStatusReportV16;
import com.stan.poc.spi2iso.jaxb.generated.pacs002.GroupHeader120;
import com.stan.poc.spi2iso.jaxb.generated.pacs002.PaymentTransaction177;
import com.stan.poc.spi2iso.jaxb.generated.pacs002.StatusReason6Choice;
import com.stan.poc.spi2iso.jaxb.generated.pacs002.StatusReasonInformation14;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.Document;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Simula el comportamiento del switch/conmutador del BCE (SPI / futura RPI) frente a un
 * mensaje pacs.008 entrante, devolviendo un pacs.002 (Payment Status Report) - ambos
 * generados por xjc a partir del XSD oficial de iso20022.org (ver README.md).
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

    public SimulatedResponse procesar(Document pacs008) {
        var tx = pacs008.getFIToFICstmrCdtTrf().getCdtTrfTxInf().get(0);

        String ustrd = (tx.getRmtInf() != null && !tx.getRmtInf().getUstrd().isEmpty())
                ? tx.getRmtInf().getUstrd().get(0)
                : null;
        if (ustrd != null && ustrd.toUpperCase().contains("TIMEOUT")) {
            return new SimulatedResponse(RESULT_TIMEOUT, null);
        }

        boolean forzarRechazo = tx.getIntrBkSttlmAmt() != null
                && tx.getIntrBkSttlmAmt().getValue() != null
                && tx.getIntrBkSttlmAmt().getValue().doubleValue() == 999.99d;

        var estado = new PaymentTransaction177();
        estado.setOrgnlInstrId(tx.getPmtId().getInstrId());
        estado.setOrgnlEndToEndId(tx.getPmtId().getEndToEndId());

        if (forzarRechazo) {
            estado.setTxSts("RJCT");
            var rsnInf = new StatusReasonInformation14();
            var rsn = new StatusReason6Choice();
            rsn.setCd("AM02"); // "NotAllowedAmount" - codigo ISO 20022 estandar de motivo de rechazo
            rsnInf.setRsn(rsn);
            rsnInf.getAddtlInf().add("Monto no permitido (regla de demo del simulador de switch)");
            estado.getStsRsnInf().add(rsnInf);
        } else {
            estado.setTxSts("ACSC");
        }

        var grpHdr = new GroupHeader120();
        grpHdr.setMsgId("RESP-" + UUID.randomUUID());
        grpHdr.setCreDtTm(XmlUtil.toXmlDateTime(OffsetDateTime.now(ZoneOffset.UTC)));

        var body = new FIToFIPaymentStatusReportV16();
        body.setGrpHdr(grpHdr);
        body.getTxInfAndSts().add(estado);

        var doc = new com.stan.poc.spi2iso.jaxb.generated.pacs002.Document();
        doc.setFIToFIPmtStsRpt(body);

        return new SimulatedResponse(estado.getTxSts(), doc);
    }

    public record SimulatedResponse(String estado, com.stan.poc.spi2iso.jaxb.generated.pacs002.Document pacs002) {
    }
}
