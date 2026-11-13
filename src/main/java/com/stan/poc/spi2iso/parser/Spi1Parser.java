package com.stan.poc.spi2iso.parser;

import com.stan.poc.spi2iso.model.Spi1Record;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser del archivo SPI1 de este PoC.
 *
 * Formato adoptado (delimitado por "|", una transaccion por linea, UTF-8):
 *
 *   numeroInstruccion|institucionOrdenanteCodigo|institucionOrdenanteNombre|
 *   institucionReceptoraCodigo|institucionReceptoraNombre|cuentaOrigen|cuentaDestino|
 *   monto|moneda|tipoIdOrdenante|idOrdenante|nombreOrdenante|
 *   tipoIdBeneficiario|idBeneficiario|nombreBeneficiario|fechaHora(yyyyMMddHHmmss)|referencia
 *
 * Nota honesta: el BCE no publica el layout exacto del SPI1 real (posiciones/anchos de
 * campo fijo que usa hoy la banca ecuatoriana) en una fuente abierta. Este parser define
 * un formato delimitado razonable, con los MISMOS campos de negocio que exige la
 * migracion (incluido el campo de identificacion del ordenante exigido desde el 6 de
 * julio de 2026 por el Art. 52 LAFT), para poder demostrar el motor de transformacion de
 * punta a punta. Ver README.md.
 */
@Component
public class Spi1Parser {

    private static final String DELIM = "\\|";
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public List<Spi1Record> parseFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<Spi1Record> records = new ArrayList<>();
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            if (line.isBlank() || line.startsWith("#")) {
                continue; // comentarios / lineas vacias permitidas para legibilidad del archivo de ejemplo
            }
            try {
                records.add(parseLine(line));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Error parseando linea " + lineNo + " del archivo SPI1: " + e.getMessage(), e);
            }
        }
        return records;
    }

    public Spi1Record parseLine(String line) {
        String[] f = line.split(DELIM, -1);
        if (f.length < 17) {
            throw new IllegalArgumentException(
                    "Se esperaban 17 campos delimitados por '|', se encontraron " + f.length);
        }

        Spi1Record r = new Spi1Record();
        int i = 0;
        r.setNumeroInstruccion(trim(f[i++]));
        r.setInstitucionOrdenanteCodigo(trim(f[i++]));
        r.setInstitucionOrdenanteNombre(trim(f[i++]));
        r.setInstitucionReceptoraCodigo(trim(f[i++]));
        r.setInstitucionReceptoraNombre(trim(f[i++]));
        r.setCuentaOrigen(trim(f[i++]));
        r.setCuentaDestino(trim(f[i++]));
        r.setMonto(new BigDecimal(trim(f[i++])));
        r.setMoneda(trim(f[i++]));
        r.setTipoIdOrdenante(trim(f[i++]));
        r.setIdOrdenante(trim(f[i++]));
        r.setNombreOrdenante(trim(f[i++]));
        r.setTipoIdBeneficiario(trim(f[i++]));
        r.setIdBeneficiario(trim(f[i++]));
        r.setNombreBeneficiario(trim(f[i++]));
        r.setFechaHora(LocalDateTime.parse(trim(f[i++]), FECHA_FMT));
        r.setReferencia(trim(f[i]));
        return r;
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
