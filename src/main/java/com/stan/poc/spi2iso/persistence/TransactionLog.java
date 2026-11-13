package com.stan.poc.spi2iso.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro de trazabilidad de cada transaccion procesada por el motor.
 *
 * Diseno alineado con la Capa 4 (Seguridad) del documento del PoC: log append-only
 * (sin update/delete expuesto), con marcas de tiempo por etapa (estilo T510/T520) y
 * enmascaramiento del numero de identificacion del ordenante/beneficiario en el campo
 * mostrado en consola/README (el dato completo SI se persiste aqui, como lo haria un
 * sistema real sujeto a retencion de 10 anios por el Art. 52 LAFT; el enmascaramiento
 * es solo para lo que se imprime en logs de demo, no para lo que se guarda).
 */
@Entity
@Table(name = "transaction_log")
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroInstruccion;

    @Column(nullable = false)
    private String estado; // RECIBIDO | VALIDADO | RECHAZADO | ENVIADO | ACSC | RJCT | TIMEOUT

    @Column(length = 2000)
    private String detalle;

    @Lob
    private String spi1Original;

    @Lob
    private String pacs008Xml;

    @Lob
    private String pacs002Xml;

    @Column(nullable = false)
    private LocalDateTime t510RecibidoEn;

    private LocalDateTime t515ValidadoEn;

    private LocalDateTime t520EnviadoEn;

    private LocalDateTime t530RespuestaEn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroInstruccion() {
        return numeroInstruccion;
    }

    public void setNumeroInstruccion(String numeroInstruccion) {
        this.numeroInstruccion = numeroInstruccion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getSpi1Original() {
        return spi1Original;
    }

    public void setSpi1Original(String spi1Original) {
        this.spi1Original = spi1Original;
    }

    public String getPacs008Xml() {
        return pacs008Xml;
    }

    public void setPacs008Xml(String pacs008Xml) {
        this.pacs008Xml = pacs008Xml;
    }

    public String getPacs002Xml() {
        return pacs002Xml;
    }

    public void setPacs002Xml(String pacs002Xml) {
        this.pacs002Xml = pacs002Xml;
    }

    public LocalDateTime getT510RecibidoEn() {
        return t510RecibidoEn;
    }

    public void setT510RecibidoEn(LocalDateTime t510RecibidoEn) {
        this.t510RecibidoEn = t510RecibidoEn;
    }

    public LocalDateTime getT515ValidadoEn() {
        return t515ValidadoEn;
    }

    public void setT515ValidadoEn(LocalDateTime t515ValidadoEn) {
        this.t515ValidadoEn = t515ValidadoEn;
    }

    public LocalDateTime getT520EnviadoEn() {
        return t520EnviadoEn;
    }

    public void setT520EnviadoEn(LocalDateTime t520EnviadoEn) {
        this.t520EnviadoEn = t520EnviadoEn;
    }

    public LocalDateTime getT530RespuestaEn() {
        return t530RespuestaEn;
    }

    public void setT530RespuestaEn(LocalDateTime t530RespuestaEn) {
        this.t530RespuestaEn = t530RespuestaEn;
    }
}
