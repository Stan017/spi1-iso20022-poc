package com.stan.poc.spi2iso.controller.bian;

import com.stan.poc.spi2iso.persistence.TransactionLog;
import com.stan.poc.spi2iso.persistence.TransactionLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API expuesta como el Service Domain BIAN <b>Clearing and Settlement</b>.
 *
 * En este PoC, este dominio es cubierto funcionalmente por {@code SwitchGatewaySimulator}
 * (simula al switch del BCE que liquida o rechaza la instrucción recibida desde
 * Payment Execution) y su resultado queda grabado en el mismo Control Record
 * ({@link TransactionLog}) que usa Payment Execution — en una arquitectura real estos
 * serían dos Service Domains con sus propios Control Records, correlacionados por un
 * identificador compartido (aquí, simplificado, es el mismo registro).
 *
 * Behavior Qualifier de BIAN cubierto: <b>Retrieve</b> — consulta el estado de
 * liquidación/clearing (ACSC / RJCT / TIMEOUT) y el mensaje pacs.002 asociado, sin
 * expone los detalles internos de Payment Execution (el pacs.008 de salida).
 *
 * No implementado (fuera de alcance del PoC): Initiate propio de este dominio (en
 * este PoC, Clearing and Settlement reacciona a lo que Payment Execution le envía,
 * no expone un Initiate público independiente), Control, Exchange, Notify.
 */
@Tag(name = "Clearing and Settlement",
        description = "BIAN Service Domain: Clearing and Settlement — Behavior Qualifier Retrieve. " +
                "Expone la vista de liquidación del switch simulado (pacs.002 / estado ACSC / RJCT / TIMEOUT). " +
                "Requiere scope `transferencias:read`.")
@RestController
@RequestMapping("/clearing-and-settlement/settlement-status")
public class ClearingAndSettlementController {

    private final TransactionLogRepository repository;

    public ClearingAndSettlementController(TransactionLogRepository repository) {
        this.repository = repository;
    }

    @Operation(
            summary = "Retrieve — estado de liquidación por CRId",
            description = """
                    **BIAN BQ: Retrieve.** Devuelve la vista de liquidación/clearing del Control Record: \
                    estado (`ACSC` = liquidado, `RJCT` = rechazado, `TIMEOUT`), el pacs.002 XML recibido \
                    del switch simulado y los timestamps T520 / T530.

                    Deliberadamente **no expone** el pacs.008 ni los datos crudos del SPI1 original \
                    (esos pertenecen al Service Domain Payment Execution).
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Vista de liquidación encontrada",
                            content = @Content(schema = @Schema(implementation = SettlementStatusView.class))),
                    @ApiResponse(responseCode = "404", description = "CRId no encontrado", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Sin token o token inválido", content = @Content)
            }
    )
    @GetMapping("/{crId}/retrieve")
    public ResponseEntity<SettlementStatusView> retrieve(
            @Parameter(description = "Control Record Id — el numeroInstruccion del SPI1 "
                    + "(ej. INS-0001=ACSC, INS-0002=RJCT, INS-0003=TIMEOUT en examples/spi1-demo.txt)",
                    example = "INS-0002")
            @PathVariable String crId) {
        return repository.findByNumeroInstruccion(crId)
                .map(this::toSettlementView)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private SettlementStatusView toSettlementView(TransactionLog log) {
        return new SettlementStatusView(
                log.getNumeroInstruccion(),
                log.getEstado(),
                log.getDetalle(),
                log.getPacs002Xml(),
                log.getT520EnviadoEn(),
                log.getT530RespuestaEn()
        );
    }

    /**
     * Vista de solo-lectura, propia del dominio Clearing and Settlement, sobre el
     * Control Record compartido: deliberadamente NO expone el pacs.008 (eso
     * pertenece a la vista de Payment Execution) ni los datos crudos del SPI1
     * original.
     */
    public record SettlementStatusView(
            String crId,
            String estadoLiquidacion,
            String detalle,
            String pacs002Xml,
            java.time.LocalDateTime enviadoACompensacionEn,
            java.time.LocalDateTime respuestaRecibidaEn) {
    }
}
