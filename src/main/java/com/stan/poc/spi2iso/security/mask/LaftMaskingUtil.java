package com.stan.poc.spi2iso.security.mask;

/**
 * Enmascaramiento de datos sensibles (numero de identificacion del ordenante/
 * beneficiario) para lo que se IMPRIME o muestra en logs/consola de la demo.
 *
 * Alcance (ver README, Capa 4 - Seguridad): el dato completo SI se persiste en
 * TransactionLog (igual que lo haria un sistema real sujeto a retencion de 10 anios
 * por el Art. 52 de la Ley LAFT) - el enmascaramiento es exclusivamente para reducir
 * la exposicion de datos sensibles en salidas de bajo nivel de confianza (logs de
 * aplicacion, consola, salida de demo), no para lo que se guarda en la base.
 *
 * Centralizado aqui (en lugar de repetido en cada punto que loguea) para que la
 * regla de enmascaramiento sea una sola y auditable.
 */
public final class LaftMaskingUtil {

    private static final int CARACTERES_VISIBLES = 4;
    private static final String RELLENO = "*";

    private LaftMaskingUtil() {
    }

    /**
     * Enmascara un identificador dejando visibles solo los ultimos
     * {@value #CARACTERES_VISIBLES} caracteres (ej. "1712345678" -> "******5678").
     * Identificadores de 4 caracteres o menos se enmascaran por completo.
     */
    public static String enmascararIdentificacion(String valor) {
        if (valor == null || valor.isBlank()) {
            return "(no provisto)";
        }
        String limpio = valor.trim();
        if (limpio.length() <= CARACTERES_VISIBLES) {
            return RELLENO.repeat(4);
        }
        return RELLENO.repeat(limpio.length() - CARACTERES_VISIBLES) + limpio.substring(limpio.length() - CARACTERES_VISIBLES);
    }

    /** Enmascara un numero de cuenta de la misma forma, para consistencia en los logs de demo. */
    public static String enmascararCuenta(String valor) {
        return enmascararIdentificacion(valor);
    }
}
