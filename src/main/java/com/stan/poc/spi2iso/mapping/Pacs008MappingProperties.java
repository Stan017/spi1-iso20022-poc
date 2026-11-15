package com.stan.poc.spi2iso.mapping;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Valores de mapeo SPI1 -> pacs.008 que HOY son un supuesto razonado (ver README,
 * seccion "Alcance, fuentes y supuestos" #3 y #4), no una especificacion publicada
 * por el BCE. Se exponen como configuracion (poc.mapping.pacs008.* en
 * application.properties) en vez de constantes/literales dentro de
 * {@link Pacs008Mapper} para que, cuando el BCE publique su especificacion de
 * campos, el ajuste sea un cambio de configuracion - no una reescritura del mapper.
 */
@Component
@ConfigurationProperties(prefix = "poc.mapping.pacs008")
public class Pacs008MappingProperties {

    /** ChrgBr: el SPI1 de este PoC no modela reparto de comisiones. */
    private String chargeBearer = "SLEV";

    /** SttlmMtd: el SPI1 de este PoC no modela el metodo de liquidacion. */
    private String settlementMethod = "CLRG";

    /**
     * Traduce el tipo de identificacion del SPI1 (CI, CE, RUC, PASS) al codigo que
     * se emite en Dbtr/Cdtr -&gt; Id/PrvtId/Othr/SchmeNm/Prtry. Por defecto es
     * identidad (el codigo SPI1 se usa tal cual) porque no existe un catalogo
     * oficial del BCE - ver supuesto #4 en el README (se uso como referencia el
     * catalogo publico de Local Payment). Si se obtiene el catalogo oficial del
     * SRI/BCE, basta con sobreescribir estas entradas aqui, nunca tocar
     * Pacs008Mapper.
     */
    private Map<String, String> identificationTypes = new LinkedHashMap<>(Map.of(
            "CI", "CI",
            "CE", "CE",
            "RUC", "RUC",
            "PASS", "PASS"));

    public String getChargeBearer() {
        return chargeBearer;
    }

    public void setChargeBearer(String chargeBearer) {
        this.chargeBearer = chargeBearer;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public Map<String, String> getIdentificationTypes() {
        return identificationTypes;
    }

    public void setIdentificationTypes(Map<String, String> identificationTypes) {
        this.identificationTypes = identificationTypes;
    }

    /**
     * Traduce un tipoId del SPI1 a su codigo de salida configurado; si no hay
     * entrada configurada para el, lo deja igual (fallback seguro, nunca lanza).
     */
    public String translateIdentificationType(String tipoIdSpi1) {
        if (tipoIdSpi1 == null) {
            return null;
        }
        return identificationTypes.getOrDefault(tipoIdSpi1, tipoIdSpi1);
    }
}
