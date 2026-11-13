package com.stan.poc.spi2iso;

import com.stan.poc.spi2iso.gateway.SwitchGatewaySimulator;
import com.stan.poc.spi2iso.jaxb.XmlUtil;
import com.stan.poc.spi2iso.jaxb.generated.pacs008.Document;
import com.stan.poc.spi2iso.mapping.Pacs008Mapper;
import com.stan.poc.spi2iso.model.Spi1Record;
import com.stan.poc.spi2iso.parser.Spi1Parser;
import com.stan.poc.spi2iso.validation.Pacs008Validator;
import com.stan.poc.spi2iso.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de humo (sin levantar el contexto de Spring) del pipeline de transformacion:
 * parseo -> validacion -> mapeo -> marshalling XML -> simulador de switch.
 */
class PipelineTest {

    private final Spi1Parser parser = new Spi1Parser();
    private final Pacs008Validator validator = new Pacs008Validator();
    private final Pacs008Mapper mapper = new Pacs008Mapper();
    private final SwitchGatewaySimulator gateway = new SwitchGatewaySimulator();

    private static final String LINEA_VALIDA =
            "INS-TEST-1|0010|Banco Demo Uno|0020|Banco Demo Dos|01-0123456-7|02-9876543-1|250.00|USD|" +
                    "CI|1712345678|Maria Fernanda Lopez|RUC|1790012345001|Comercial Andina S.A.|" +
                    "20261112093000|Pago factura 4521";

    @Test
    void parseaUnaLineaValida() {
        Spi1Record r = parser.parseLine(LINEA_VALIDA);
        assertEquals("INS-TEST-1", r.getNumeroInstruccion());
        assertEquals(new BigDecimal("250.00"), r.getMonto());
        assertEquals("CI", r.getTipoIdOrdenante());
    }

    @Test
    void registroValidoPasaValidacion() {
        Spi1Record r = parser.parseLine(LINEA_VALIDA);
        ValidationResult result = validator.validate(r);
        assertTrue(result.isValid(), "Se esperaba un registro valido: " + result);
    }

    @Test
    void registroSinTipoIdOrdenanteEsRechazado() {
        Spi1Record r = parser.parseLine(LINEA_VALIDA);
        r.setTipoIdOrdenante(null);
        ValidationResult result = validator.validate(r);
        assertFalse(result.isValid());
        assertTrue(result.toString().contains("SPI-020"));
    }

    @Test
    void montoSuperiorA15000EsRechazado() {
        Spi1Record r = parser.parseLine(LINEA_VALIDA);
        r.setMonto(new BigDecimal("20000.00"));
        ValidationResult result = validator.validate(r);
        assertFalse(result.isValid());
        assertTrue(result.toString().contains("SPI-012"));
    }

    @Test
    void mapeoGeneraXmlPacs008ConCamposClave() {
        Spi1Record r = parser.parseLine(LINEA_VALIDA);
        Document doc = mapper.toPacs008(r);
        String xml = XmlUtil.marshalPacs008(doc);

        assertTrue(xml.contains("pacs.008.001.14"));
        assertTrue(xml.contains("IntrBkSttlmAmt"));
        assertTrue(xml.contains("1712345678")); // id del ordenante propagado
        assertTrue(xml.contains("Comercial Andina S.A."));
    }

    @Test
    void switchSimuladoAceptaTransaccionNormal() {
        Spi1Record r = parser.parseLine(LINEA_VALIDA);
        Document doc = mapper.toPacs008(r);
        var respuesta = gateway.procesar(doc);
        assertEquals("ACSC", respuesta.estado());
    }

    @Test
    void switchSimuladoRechazaMontoDeDemo999_99() {
        Spi1Record r = parser.parseLine(LINEA_VALIDA);
        r.setMonto(new BigDecimal("999.99"));
        Document doc = mapper.toPacs008(r);
        var respuesta = gateway.procesar(doc);
        assertEquals("RJCT", respuesta.estado());
    }

    @Test
    void switchSimuladoNoRespondeConReferenciaTimeout() {
        Spi1Record r = parser.parseLine(LINEA_VALIDA);
        r.setReferencia("TIMEOUT de prueba");
        Document doc = mapper.toPacs008(r);
        var respuesta = gateway.procesar(doc);
        assertEquals(SwitchGatewaySimulator.RESULT_TIMEOUT, respuesta.estado());
        assertNull(respuesta.pacs002());
    }
}
