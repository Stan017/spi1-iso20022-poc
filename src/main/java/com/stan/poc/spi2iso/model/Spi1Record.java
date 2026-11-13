package com.stan.poc.spi2iso.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un registro (una transaccion) dentro de un archivo SPI1 - el formato de
 * archivo plano por lotes que las entidades financieras ecuatorianas intercambian hoy
 * con el Banco Central del Ecuador (BCE) para instruir transferencias interbancarias
 * dentro del Sistema de Pagos Interbancarios (SPI).
 *
 * El layout de campos exacto del SPI1 real (posiciones, anchos, delimitador) no esta
 * publicado por el BCE en una fuente publica accesible desde este entorno. Este POJO
 * modela los campos que SI estan confirmados por los documentos regulatorios citados
 * en el README (Circular BCE-GMPSN-2026-008-C / 013-C) mas los campos minimos
 * necesarios para construir un mensaje pacs.008 valido. Ver README.md, seccion
 * "Fuentes y supuestos".
 */
public class Spi1Record {

    /** Codigo de la institucion financiera ordenante (ej. codigo BCE de la entidad origen). */
    private String institucionOrdenanteCodigo;
    private String institucionOrdenanteNombre;

    /** Codigo de la institucion financiera receptora/beneficiaria. */
    private String institucionReceptoraCodigo;
    private String institucionReceptoraNombre;

    private String cuentaOrigen;
    private String cuentaDestino;

    private BigDecimal monto;
    private String moneda; // por defecto USD (dolarizacion Ecuador)

    /** Tipo de identificacion del ordenante: CI, CE, RUC o PASS (ver README). */
    private String tipoIdOrdenante;
    private String idOrdenante;
    private String nombreOrdenante;

    /** Tipo y datos del beneficiario, mismo catalogo que el ordenante. */
    private String tipoIdBeneficiario;
    private String idBeneficiario;
    private String nombreBeneficiario;

    private LocalDateTime fechaHora;

    /** Referencia / glosa de la transaccion, y numero de instruccion unico del lote. */
    private String referencia;
    private String numeroInstruccion;

    public Spi1Record() {
    }

    // --- getters y setters ---

    public String getInstitucionOrdenanteCodigo() {
        return institucionOrdenanteCodigo;
    }

    public void setInstitucionOrdenanteCodigo(String institucionOrdenanteCodigo) {
        this.institucionOrdenanteCodigo = institucionOrdenanteCodigo;
    }

    public String getInstitucionOrdenanteNombre() {
        return institucionOrdenanteNombre;
    }

    public void setInstitucionOrdenanteNombre(String institucionOrdenanteNombre) {
        this.institucionOrdenanteNombre = institucionOrdenanteNombre;
    }

    public String getInstitucionReceptoraCodigo() {
        return institucionReceptoraCodigo;
    }

    public void setInstitucionReceptoraCodigo(String institucionReceptoraCodigo) {
        this.institucionReceptoraCodigo = institucionReceptoraCodigo;
    }

    public String getInstitucionReceptoraNombre() {
        return institucionReceptoraNombre;
    }

    public void setInstitucionReceptoraNombre(String institucionReceptoraNombre) {
        this.institucionReceptoraNombre = institucionReceptoraNombre;
    }

    public String getCuentaOrigen() {
        return cuentaOrigen;
    }

    public void setCuentaOrigen(String cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public void setCuentaDestino(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getTipoIdOrdenante() {
        return tipoIdOrdenante;
    }

    public void setTipoIdOrdenante(String tipoIdOrdenante) {
        this.tipoIdOrdenante = tipoIdOrdenante;
    }

    public String getIdOrdenante() {
        return idOrdenante;
    }

    public void setIdOrdenante(String idOrdenante) {
        this.idOrdenante = idOrdenante;
    }

    public String getNombreOrdenante() {
        return nombreOrdenante;
    }

    public void setNombreOrdenante(String nombreOrdenante) {
        this.nombreOrdenante = nombreOrdenante;
    }

    public String getTipoIdBeneficiario() {
        return tipoIdBeneficiario;
    }

    public void setTipoIdBeneficiario(String tipoIdBeneficiario) {
        this.tipoIdBeneficiario = tipoIdBeneficiario;
    }

    public String getIdBeneficiario() {
        return idBeneficiario;
    }

    public void setIdBeneficiario(String idBeneficiario) {
        this.idBeneficiario = idBeneficiario;
    }

    public String getNombreBeneficiario() {
        return nombreBeneficiario;
    }

    public void setNombreBeneficiario(String nombreBeneficiario) {
        this.nombreBeneficiario = nombreBeneficiario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getNumeroInstruccion() {
        return numeroInstruccion;
    }

    public void setNumeroInstruccion(String numeroInstruccion) {
        this.numeroInstruccion = numeroInstruccion;
    }

    @Override
    public String toString() {
        return "Spi1Record{" +
                "numeroInstruccion='" + numeroInstruccion + '\'' +
                ", institucionOrdenanteCodigo='" + institucionOrdenanteCodigo + '\'' +
                ", institucionReceptoraCodigo='" + institucionReceptoraCodigo + '\'' +
                ", monto=" + monto +
                ", moneda='" + moneda + '\'' +
                ", tipoIdOrdenante='" + tipoIdOrdenante + '\'' +
                '}';
    }
}
