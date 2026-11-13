package com.stan.poc.spi2iso.generator;

import com.stan.poc.spi2iso.model.Spi1Record;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Genera lineas/archivos SPI1 de prueba (formato definido en Spi1Parser) para usar
 * en la demo: un escenario exitoso, uno rechazado por validacion y uno pensado para
 * forzar un timeout simulado del switch.
 */
@Component
public class Spi1FileGenerator {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String toLine(Spi1Record r) {
        return String.join("|",
                nullToEmpty(r.getNumeroInstruccion()),
                nullToEmpty(r.getInstitucionOrdenanteCodigo()),
                nullToEmpty(r.getInstitucionOrdenanteNombre()),
                nullToEmpty(r.getInstitucionReceptoraCodigo()),
                nullToEmpty(r.getInstitucionReceptoraNombre()),
                nullToEmpty(r.getCuentaOrigen()),
                nullToEmpty(r.getCuentaDestino()),
                r.getMonto() == null ? "" : r.getMonto().toPlainString(),
                nullToEmpty(r.getMoneda()),
                nullToEmpty(r.getTipoIdOrdenante()),
                nullToEmpty(r.getIdOrdenante()),
                nullToEmpty(r.getNombreOrdenante()),
                nullToEmpty(r.getTipoIdBeneficiario()),
                nullToEmpty(r.getIdBeneficiario()),
                nullToEmpty(r.getNombreBeneficiario()),
                r.getFechaHora() == null ? "" : r.getFechaHora().format(FECHA_FMT),
                nullToEmpty(r.getReferencia())
        );
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
