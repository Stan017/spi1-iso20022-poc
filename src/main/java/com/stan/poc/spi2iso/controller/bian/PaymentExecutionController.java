package com.stan.poc.spi2iso.controller.bian;

import com.stan.poc.spi2iso.model.Spi1Record;
import com.stan.poc.spi2iso.parser.Spi1Parser;
import com.stan.poc.spi2iso.persistence.TransactionLog;
import com.stan.poc.spi2iso.persistence.TransactionLogRepository;
import com.stan.poc.spi2iso.service.Spi1ToIso20022Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * API expuesta como el Service Domain BIAN <b>Payment Execution</b>.
 *
 * Este dominio es responsable de recibir, validar y procesar la instrucción de pago
 * de punta a punta hasta entregarla al dominio de liquidación (Clearing and
 * Settlement, ver {@link ClearingAndSettlementController}). En este PoC, el
 * <b>Control Record</b> de Payment Execution (el objeto de negocio central cuyo
 * ciclo de vida se rastrea) es la entidad {@link TransactionLog}, identificado por
 * su {@code numeroInstruccion} — que aquí hace de CRId (Control Record Id) en el
 * sentido de BIAN.
 *
 * Behavior Qualifiers de BIAN cubiertos por este PoC:
 *   - <b>Initiate</b>: {@code POST .../payment-instructions/initiate} y
 *     {@code .../initiate-batch} — crea un nuevo Control Record y ejecuta el
 *     pipeline (parseo → validación → mapeo → envío a Clearing and Settlement).
 *   - <b>Retrieve</b>: {@code GET .../payment-instructions/{crId}/retrieve} y
 *     {@code GET .../payment-instructions/retrieve} (todas) — consulta el estado
 *     actual del Control Record, sin modificarlo.
 *
 * Behavior Qualifiers de BIAN NO implementados en este PoC (quedan fuera de alcance
 * de un PoC de portafolio, ver README): Update (modificar una instrucción ya
 * iniciada), Control (suspender/reanudar/cancelar), Exchange y Notify (eventos
 * asíncronos hacia otros Service Domains).
 */
@Tag(name = "Payment Execution",
        description = "BIAN Service Domain: Payment Execution — Behavior Qualifiers Initiate y Retrieve. " +
                "Requiere scope `transferencias:write` (POST) o `transferencias:read` (GET).")
@RestController
@RequestMapping("/payment-execution/payment-instructions")
public class PaymentExecutionController {

    private final Spi1ToIso20022Service service;
    private final TransactionLogRepository repository;
    private final Spi1Parser parser;

    public PaymentExecutionController(Spi1ToIso20022Service service,
                                       TransactionLogRepository repository,
                                       Spi1Parser parser) {
        this.service = service;
        this.repository = repository;
        this.parser = parser;
    }

    @Operation(
            summary = "Initiate — procesar una instrucción SPI1",
            description = """
                    **BIAN BQ: Initiate.** Recibe una línea en formato SPI1 (texto plano, delimitado por `|`), \
                    la parsea, valida, mapea a pacs.008 ISO 20022, la envía al switch simulado y persiste \
                    el log de trazabilidad (T510/T515/T520/T530).

                    Ejemplo de línea SPI1 (idéntica a la primera línea de `examples/spi1-demo.txt`,
                    17 campos delimitados por `|` — ver `Spi1Parser.java` para el detalle de cada campo):
                    ```
                    INS-0001|0010|Banco Demo Uno|0020|Banco Demo Dos|01-0123456-7|02-9876543-1|250.00|USD|CI|1712345678|Maria Fernanda Lopez|RUC|1790012345001|Comercial Andina S.A.|20261112093000|Pago factura 4521
                    ```
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(
                                    value = "INS-0001|0010|Banco Demo Uno|0020|Banco Demo Dos|01-0123456-7|02-9876543-1|250.00|USD|CI|1712345678|Maria Fernanda Lopez|RUC|1790012345001|Comercial Andina S.A.|20261112093000|Pago factura 4521"))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Procesada (ACSC, RJCT o TIMEOUT)",
                            content = @Content(schema = @Schema(implementation = Spi1ToIso20022Service.TransactionResult.class))),
                    @ApiResponse(responseCode = "401", description = "Sin token o token inválido",
                            content = @Content),
                    @ApiResponse(responseCode = "403", description = "Token sin scope transferencias:write",
                            content = @Content)
            }
    )
    @PostMapping(value = "/initiate", consumes = "text/plain")
    public ResponseEntity<Spi1ToIso20022Service.TransactionResult> initiate(@org.springframework.web.bind.annotation.RequestBody String lineaSpi1) {
        Spi1Record record = parser.parseLine(lineaSpi1);
        return ResponseEntity.ok(service.procesarRegistro(record));
    }

    @Operation(
            summary = "Initiate-batch — procesar un archivo SPI1 completo",
            description = """
                    **Conveniencia de demo** (no un BQ BIAN propio): aplica Initiate a cada línea del archivo \
                    SPI1 indicado por `ruta` (ruta del sistema de archivos local). \
                    Útil para probar el pipeline completo con el archivo de ejemplo incluido en `examples/`.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de resultados, uno por instrucción"),
                    @ApiResponse(responseCode = "401", description = "Sin token o token inválido", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Token sin scope transferencias:write", content = @Content)
            }
    )
    @PostMapping("/initiate-batch")
    public ResponseEntity<List<Spi1ToIso20022Service.TransactionResult>> initiateBatch(
            @Parameter(description = "Ruta del archivo SPI1 (absoluta, o relativa al directorio de trabajo del proceso)",
                    example = "examples/spi1-demo.txt")
            @RequestParam String ruta) throws IOException {
        return ResponseEntity.ok(service.procesarArchivo(Path.of(ruta)));
    }

    @Operation(
            summary = "Retrieve — consultar una instrucción por CRId",
            description = "**BIAN BQ: Retrieve.** Devuelve el Control Record completo (TransactionLog) " +
                    "de la instrucción identificada por `crId` (= numeroInstruccion del SPI1).",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Control Record encontrado",
                            content = @Content(schema = @Schema(implementation = TransactionLog.class))),
                    @ApiResponse(responseCode = "404", description = "CRId no encontrado", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Sin token o token inválido", content = @Content)
            }
    )
    @GetMapping("/{crId}/retrieve")
    public ResponseEntity<TransactionLog> retrieve(
            @Parameter(description = "Control Record Id — el numeroInstruccion del SPI1", example = "INS-0001")
            @PathVariable String crId) {
        return repository.findByNumeroInstruccion(crId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Retrieve-all — listar todos los Control Records",
            description = "**BIAN BQ: Retrieve** (todas las instancias). Devuelve la lista completa de " +
                    "TransactionLog en memoria. Conveniencia para inspeccionar el estado de la demo.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista (vacía si no hay instrucciones procesadas)"),
                    @ApiResponse(responseCode = "401", description = "Sin token o token inválido", content = @Content)
            }
    )
    @GetMapping("/retrieve")
    public ResponseEntity<List<TransactionLog>> retrieveAll() {
        return ResponseEntity.ok(repository.findAll());
    }
}
