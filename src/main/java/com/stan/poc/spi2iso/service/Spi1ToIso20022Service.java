package com.stan.poc.spi2iso.service;

import com.stan.poc.spi2iso.gateway.SwitchGatewaySimulator;
import com.stan.poc.spi2iso.jaxb.Pacs008Document;
import com.stan.poc.spi2iso.jaxb.XmlUtil;
import com.stan.poc.spi2iso.mapping.Pacs008Mapper;
import com.stan.poc.spi2iso.model.Spi1Record;
import com.stan.poc.spi2iso.parser.Spi1Parser;
import com.stan.poc.spi2iso.persistence.TransactionLog;
import com.stan.poc.spi2iso.persistence.TransactionLogRepository;
import com.stan.poc.spi2iso.security.mask.LaftMaskingUtil;
import com.stan.poc.spi2iso.validation.Pacs008Validator;
import com.stan.poc.spi2iso.validation.ValidationResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta el pipeline completo descrito en la Capa 3 (Arquitectura) del PoC:
 *
 *   SPI1 (archivo) -> Parser -> Validador -> Motor de mapeo -> pacs.008 (XML) ->
 *   API Gateway simulado -> Simulador de respuesta del switch -> pacs.002 (XML) ->
 *   Logger con trazabilidad (H2)
 *
 * Cada transaccion queda registrada en TransactionLog, con marcas de tiempo T510
 * (recibido), T515 (validado), T520 (enviado) y T530 (respuesta), y con el numero de
 * identificacion del ordenante/beneficiario enmascarado SOLO en el campo "detalle"
 * legible para la demo (el XML completo se conserva integro para trazabilidad real,
 * ver Capa 4 - Seguridad).
 */
@Service
public class Spi1ToIso20022Service {

    private final Spi1Parser parser;
    private final Pacs008Validator validator;
    private final Pacs008Mapper mapper;
    private final SwitchGatewaySimulator gateway;
    private final TransactionLogRepository repository;

    public Spi1ToIso20022Service(Spi1Parser parser,
                                  Pacs008Validator validator,
                                  Pacs008Mapper mapper,
                                  SwitchGatewaySimulator gateway,
                                  TransactionLogRepository repository) {
        this.parser = parser;
        this.validator = validator;
        this.mapper = mapper;
        this.gateway = gateway;
        this.repository = repository;
    }

    public List<TransactionResult> procesarArchivo(Path archivoSpi1) throws IOException {
        List<Spi1Record> registros = parser.parseFile(archivoSpi1);
        List<TransactionResult> resultados = new ArrayList<>();
        for (Spi1Record r : registros) {
            resultados.add(procesarRegistro(r));
        }
        return resultados;
    }

    public TransactionResult procesarRegistro(Spi1Record r) {
        TransactionLog log = new TransactionLog();
        log.setNumeroInstruccion(r.getNumeroInstruccion());
        log.setT510RecibidoEn(LocalDateTime.now());
        log.setSpi1Original(resumenEnmascarado(r));
        log.setEstado("RECIBIDO");

        ValidationResult validacion = validator.validate(r);
        log.setT515ValidadoEn(LocalDateTime.now());

        if (!validacion.isValid()) {
            log.setEstado("RECHAZADO");
            log.setDetalle("Rechazado en validacion previa al envio: " + validacion);
            repository.save(log);
            return new TransactionResult(r.getNumeroInstruccion(), "RECHAZADO", validacion.toString(), null, null);
        }

        Pacs008Document pacs008 = mapper.toPacs008(r);
        String pacs008Xml = XmlUtil.marshal(pacs008);
        log.setPacs008Xml(pacs008Xml);
        log.setEstado("ENVIADO");
        log.setT520EnviadoEn(LocalDateTime.now());

        SwitchGatewaySimulator.SimulatedResponse respuesta = gateway.procesar(pacs008);
        log.setT530RespuestaEn(LocalDateTime.now());

        if (SwitchGatewaySimulator.RESULT_TIMEOUT.equals(respuesta.estado())) {
            log.setEstado("TIMEOUT");
            log.setDetalle("El switch simulado no respondio dentro del plazo esperado (escenario de demo).");
            repository.save(log);
            return new TransactionResult(r.getNumeroInstruccion(), "TIMEOUT",
                    "Sin respuesta del switch (timeout simulado)", pacs008Xml, null);
        }

        String pacs002Xml = XmlUtil.marshal(respuesta.pacs002());
        log.setPacs002Xml(pacs002Xml);
        log.setEstado(respuesta.estado());
        if ("RJCT".equals(respuesta.estado())) {
            log.setDetalle("Rechazado por el switch simulado (ver pacs.002 / StsRsnInf).");
        } else {
            log.setDetalle("Liquidado correctamente (ACSC).");
        }

        repository.save(log);
        return new TransactionResult(r.getNumeroInstruccion(), respuesta.estado(), log.getDetalle(),
                pacs008Xml, pacs002Xml);
    }

    private String resumenEnmascarado(Spi1Record r) {
        String idEnmascarado = LaftMaskingUtil.enmascararIdentificacion(r.getIdOrdenante());
        String cuentaOrigenEnmascarada = LaftMaskingUtil.enmascararCuenta(r.getCuentaOrigen());
        return "numeroInstruccion=" + r.getNumeroInstruccion()
                + ", institucionOrdenanteCodigo=" + r.getInstitucionOrdenanteCodigo()
                + ", institucionReceptoraCodigo=" + r.getInstitucionReceptoraCodigo()
                + ", monto=" + r.getMonto() + " " + r.getMoneda()
                + ", tipoIdOrdenante=" + r.getTipoIdOrdenante()
                + ", idOrdenante=" + idEnmascarado
                + ", cuentaOrigen=" + cuentaOrigenEnmascarada;
    }

    public record TransactionResult(String numeroInstruccion, String estado, String detalle,
                                     String pacs008Xml, String pacs002Xml) {
    }
}
