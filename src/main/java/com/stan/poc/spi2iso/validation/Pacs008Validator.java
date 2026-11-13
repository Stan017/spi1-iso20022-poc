package com.stan.poc.spi2iso.validation;

import com.stan.poc.spi2iso.model.Spi1Record;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Validaciones minimas sobre el Spi1Record ANTES de mapearlo/enviarlo como pacs.008.
 *
 * Incluye explicitamente la validacion del campo de identificacion del ordenante exigido
 * desde el 6 de julio de 2026 (Circular BCE-GMPSN-2026-008-C / 013-C, Art. 52 LAFT):
 * tipo + numero de identificacion no pueden faltar, y el tipo debe ser uno de los
 * codigos documentados (CI, CE, RUC, PASS - ver README para la fuente y la salvedad
 * sobre el catalogo oficial del SRI).
 */
@Component
public class Pacs008Validator {

    private static final Set<String> TIPOS_ID_VALIDOS = Set.of("CI", "CE", "RUC", "PASS");

    public ValidationResult validate(Spi1Record r) {
        ValidationResult result = new ValidationResult();

        requireNotBlank(result, "SPI-001", "numeroInstruccion", r.getNumeroInstruccion());
        requireNotBlank(result, "SPI-002", "institucionOrdenanteCodigo", r.getInstitucionOrdenanteCodigo());
        requireNotBlank(result, "SPI-003", "institucionReceptoraCodigo", r.getInstitucionReceptoraCodigo());
        requireNotBlank(result, "SPI-004", "cuentaOrigen", r.getCuentaOrigen());
        requireNotBlank(result, "SPI-005", "cuentaDestino", r.getCuentaDestino());

        if (r.getMonto() == null) {
            result.addError("SPI-010", "monto es obligatorio");
        } else if (r.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("SPI-011", "monto debe ser mayor a cero");
        } else if (r.getMonto().compareTo(new BigDecimal("15000")) > 0) {
            // Limite de USD 15,000 segun Resolucion BCE-GG-007-2026 para pagos en tiempo real via ISO 20022.
            result.addError("SPI-012",
                    "monto supera el limite de USD 15,000 definido en la Resolucion BCE-GG-007-2026 para este canal");
        }

        requireNotBlank(result, "SPI-013", "moneda", r.getMoneda());
        if (r.getMoneda() != null && !"USD".equalsIgnoreCase(r.getMoneda())) {
            result.addError("SPI-014", "moneda '" + r.getMoneda() + "' no soportada en este PoC (solo USD)");
        }

        // Campo obligatorio desde el 6-jul-2026 (Art. 52 LAFT).
        requireNotBlank(result, "SPI-020", "tipoIdOrdenante", r.getTipoIdOrdenante());
        requireNotBlank(result, "SPI-021", "idOrdenante", r.getIdOrdenante());
        if (r.getTipoIdOrdenante() != null && !TIPOS_ID_VALIDOS.contains(r.getTipoIdOrdenante().toUpperCase())) {
            result.addError("SPI-022", "tipoIdOrdenante '" + r.getTipoIdOrdenante()
                    + "' no reconocido (valores esperados: CI, CE, RUC, PASS)");
        }
        requireNotBlank(result, "SPI-023", "nombreOrdenante", r.getNombreOrdenante());

        requireNotBlank(result, "SPI-030", "idBeneficiario", r.getIdBeneficiario());
        requireNotBlank(result, "SPI-031", "nombreBeneficiario", r.getNombreBeneficiario());

        if (r.getFechaHora() == null) {
            result.addError("SPI-040", "fechaHora es obligatoria");
        }

        return result;
    }

    private void requireNotBlank(ValidationResult result, String codigo, String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            result.addError(codigo, campo + " es obligatorio");
        }
    }
}
