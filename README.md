# PoC: SPI1 → ISO 20022 Transformation Engine (Ecuador)

[![CI](https://github.com/Stan017/spi1-iso20022-poc/actions/workflows/ci.yml/badge.svg)](https://github.com/Stan017/spi1-iso20022-poc/actions/workflows/ci.yml)

> **Note on code language**: inline comments and Javadoc throughout the source code are written in Spanish, as this project was built for a technical portfolio application to a Ecuadorian financial institution (Banco Pichincha). The API contracts, field names, and all public interfaces are in English.

Portfolio project built to demonstrate **integration architecture** competencies (TOGAF 9.2, BIAN, ITIL, COBIT) applied to a real, active regulatory problem in the Ecuadorian financial system: the migration of SPI1 flat files from the Banco Central del Ecuador (BCE) to ISO 20022 messaging.

> This is a **portfolio proof of concept**, not production software nor an official implementation certified by the Banco Central del Ecuador. Read the "Scope, sources and assumptions" section before citing it as technical evidence.

## The business problem

- **BCE Resolution GG-007-2026** (March 2026): mandates ISO 20022 as the messaging standard for real-time payments up to USD 15,000. Deadline for inter-participant messaging within the payment network: **November 30, 2026**.
- **BCE Circular GMPSN-2026-008-C / 013-C**: the SPI1 file format (v2) adds a new mandatory field — the payer's national ID / RUC / passport — per Art. 52 of the Anti-Money Laundering Law (LAFT). Effective **July 6, 2026**.

Ecuadorian financial institutions must move from a **batch flat-file model with 3 daily settlement windows** to **real-time ISO 20022 messaging** — an architectural change, not merely a format change.

## What this PoC does

Implements the end-to-end transformation engine:

```
SPI1 file (flat) → Spi1Parser → Pacs008Validator → Pacs008Mapper
   → pacs.008.001.08 (real XML via JAXB) → SwitchGatewaySimulator
   → pacs.002.001.10 (real XML via JAXB) → TransactionLog (H2, with T510/T515/T520/T530 timestamps)
```

Also exposed as a REST API (Spring Boot) with 3 demo scenarios (accepted / rejected / timeout) in `examples/spi1-demo.txt`.

See also [`docs/governance.md`](docs/governance.md) for the TOGAF ADM mapping, the migration roadmap by regulatory milestone, and the project's risk matrix (Layer 5 — Governance).

## BIAN alignment

BIAN (Banking Industry Architecture Network) organizes banking capabilities into **Service Domains**. This PoC maps its pipeline to the two relevant domains for an interbank transfer, reflected directly in the code — not just in the design document:

| BIAN Service Domain | Role in this PoC | Code |
|---|---|---|
| **Payment Execution** | Receives the SPI1 instruction, validates it, transforms it to ISO 20022, and manages its lifecycle until it is handed to settlement. | `PaymentExecutionController`, `Spi1ToIso20022Service` |
| **Clearing and Settlement** | Clears or rejects the transformed instruction and returns the result (pacs.002). | `ClearingAndSettlementController`, `SwitchGatewaySimulator` |

**Control Record**: BIAN defines a Control Record for each Service Domain — the central business object whose lifecycle is tracked. In this PoC it is `TransactionLog`, identified by its `numeroInstruccion` (exposed in the API as `crId`, Control Record Id). For PoC simplicity, Payment Execution and Clearing and Settlement share the same Control Record instead of having correlated separate ones — in a real architecture these would be separate instances.

**BIAN Behavior Qualifiers** (standard actions on a Control Record) covered by the REST routes:

| Behavior Qualifier | Implemented | Where |
|---|---|---|
| **Initiate** | Yes | `POST /payment-execution/payment-instructions/initiate` (+ `initiate-batch` variant) |
| **Retrieve** | Yes | `GET .../retrieve` on both domains |
| Update | No | out of scope — modifying an already-initiated instruction |
| Control | No | out of scope — suspend/resume/cancel |
| Exchange / Notify | No | out of scope — async events between Service Domains |

This is, honestly, an **architectural/conceptual** mapping: it covers the two correct domains, the correct interaction pattern (Initiate in Payment Execution → hand-off to Clearing and Settlement → Retrieve the result), and uses real BIAN nomenclature in the routes — but it does not implement the full formal BIAN data model (Service Operations with their official XSD/OpenAPI schema, additional Behavior Qualifiers, nor separate Service Domains with their own persistence). For a technical interview, this demonstrates understanding of the framework and how to apply it, not that the full BIAN standard was implemented.

## How to run

```bash
mvn spring-boot:run -Ddemo.file=examples/spi1-demo.txt
```

This processes the sample file on startup (you will see the result of the 3 transactions in the console) and leaves the API running at `http://localhost:8080`.

To run tests only:

```bash
mvn test
```

### Continuous integration

`.github/workflows/ci.yml` runs `mvn verify` (compile + test) on every push/PR to `main` via GitHub Actions. This is especially important for this project: it was built in an environment without network access to Maven Central, so it was never compiled "live" during development — this pipeline is the first real verification, and the one that must stay green before presenting it as technical evidence.

### Endpoints

Routes follow the BIAN **Service Domain** and **Behavior Qualifier** naming convention (see "BIAN alignment" above) instead of generic names:

| Method | Route | BIAN Service Domain | Behavior Qualifier |
|---|---|---|---|
| POST | `/payment-execution/payment-instructions/initiate` (body: plain text, one SPI1 line) | Payment Execution | Initiate |
| POST | `/payment-execution/payment-instructions/initiate-batch?ruta=examples/spi1-demo.txt` | Payment Execution | Initiate (batch convenience variant) |
| GET | `/payment-execution/payment-instructions/{crId}/retrieve` | Payment Execution | Retrieve |
| GET | `/payment-execution/payment-instructions/retrieve` | Payment Execution | Retrieve (all) |
| GET | `/clearing-and-settlement/settlement-status/{crId}/retrieve` | Clearing and Settlement | Retrieve |
| — | `/swagger-ui.html` | — | Swagger UI (OpenAPI 3, no auth required) |
| — | `/h2-console` (JDBC URL `jdbc:h2:mem:spi1poc`, user `sa`) | — | H2 console for inspecting the transaction log |

`{crId}` is the Control Record Id — in this PoC, the `numeroInstruccion` from the SPI1 instruction (see `TransactionLog.java` and the BIAN section above).

All routes require the `Authorization: Bearer <token>` header (see Security section) except `/oauth/token`, `/swagger-ui.html`, `/v3/api-docs`, and `/h2-console`.

### The 3 demo scenarios

1. **`INS-0001`** — valid transaction → the simulated switch responds `ACSC` (settled).
2. **`INS-0002`** — amount `999.99` (demo rule) → the simulated switch responds `RJCT`, reason `AM02`, with the corresponding `pacs.002`.
3. **`INS-0003`** — reference containing the word `TIMEOUT` → the simulated switch does not respond, to demonstrate timeout handling in the traceability log.

## Security (Layer 4)

The PoC implements two security mechanisms, aligned with the documented design:

### 1. OAuth 2.0 (`client_credentials` flow) + JWT

API routes are protected with Spring Security. To call them, first request a token:

```bash
curl -s -X POST http://localhost:8080/oauth/token \
  -d "grant_type=client_credentials" \
  -d "client_id=banco-pichincha-poc-client" \
  -d "client_secret=demo-secret-cambiar-en-uso-real" \
  -d "scope=transferencias:read transferencias:write"
```

Returns an `access_token` (JWT) used as `Authorization: Bearer <token>` in subsequent calls:

```bash
TOKEN="<paste the access_token from above>"

curl -s http://localhost:8080/payment-execution/payment-instructions/retrieve \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://localhost:8080/payment-execution/payment-instructions/initiate-batch?ruta=examples/spi1-demo.txt" \
  -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8080/clearing-and-settlement/settlement-status/INS-0001/retrieve \
  -H "Authorization: Bearer $TOKEN"
```

- `GET .../retrieve` routes require scope `transferencias:read`.
- `POST .../initiate*` routes require scope `transferencias:write`.
- No token, or an invalid/expired/wrong-scope token → `401`/`403`.

**Honest limitation**: `TokenController` and `JwtService` simulate the *mechanics* of an OAuth2 Authorization Server (issuing an `access_token` with scopes and expiration, Bearer validation), implemented from scratch with JDK + Jackson — no JWKS, no key rotation, symmetric signing (HS256), and the secret lives in `application.properties`. In a real bank environment, the Authorization Server would be a dedicated component (Keycloak, Okta, corporate IdP) with asymmetric signing and key rotation — see the comment in `JwtService.java` for details.

### 2. mTLS (optional `mtls` profile)

To test mutual certificate authentication (API Gateway ↔ BCE switch channel, as documented in the design):

```bash
chmod +x scripts/generar-certificados-mtls.sh
./scripts/generar-certificados-mtls.sh   # generates self-signed demo certificates
mvn spring-boot:run -Dspring-boot.run.profiles=mtls
```

The API runs at `https://localhost:8443`, requiring the client to present the generated certificate (`certs/cliente.p12`). Not active by default because it requires generating those certificates first. See `application-mtls.properties` for details and limitations (mTLS here is transport only; operation identity still lives in the OAuth2/JWT token).

### 3. LAFT data masking

`LaftMaskingUtil` centralizes masking of identification numbers/account numbers printed in the demo logs (the full data is persisted in `TransactionLog`, just as a real system subject to the 10-year retention requirement under Art. 52 LAFT would do — masking applies only to what is printed in the console, not to what is stored).

### Note on startup

When starting the app, Spring Boot may print a warning about a generated password for a default `user` account (`UserDetailsServiceAutoConfiguration`). This is harmless noise: this PoC does not use that user or `httpBasic`/`formLogin` — all real authentication goes through the OAuth2/JWT flow described above.

## Stack decisions (confirmed)

| Decision | Value |
|---|---|
| Language / framework | Java 17 + Spring Boot 3.2 + Maven |
| Message format | Real XML, binding generated with `xjc` (Jakarta XML Bind) from the official iso20022.org XSD |
| Persistence | H2 embedded in-memory, JPA/Hibernate |
| Pattern vocabulary | Enterprise Integration Patterns (channel, transformer, router, validator) |
| API documentation | OpenAPI 3 / Swagger UI (`springdoc-openapi`) |

## Scope, sources and assumptions (read before citing this)

This PoC was built being deliberately honest about its limits, because that is part of what makes it defensible in front of a technical interviewer:

1. **The exact layout of the real SPI1 file is not public.** The BCE does not publish the field-position/width format used today by Ecuadorian banks in any openly accessible source. This project defines a **reasonable delimited format** (see `Spi1Parser.java`) with the same business fields mandated by the regulations cited above, in order to demonstrate the end-to-end transformation engine.

2. **The pacs.008/pacs.002 JAXB binding is generated from the real, official iso20022.org XSD** — `src/main/resources/xsd/pacs008/pacs.008.001.14.xsd` and `xsd/pacs002/pacs.002.001.16.xsd`, downloaded directly from the ISO 20022 catalogue (these are the latest published versions of each message; the field-level mapping described above still targets the business fields of pacs.008.001.08/pacs.002.001.10 cited in the regulation, which the newer versions are backward-compatible supersets of). The `jaxb2-maven-plugin` runs `xjc` on every build (`mvn generate-sources`) and regenerates the 228 classes under `target/generated-sources/jaxb` — nothing generated is committed to the repo, only the two XSD files. Two SPI1 fields the real schema requires but this project's source format doesn't carry — `ChrgBr` (charge-bearer) and `SttlmMtd` (settlement method) — are fixed to reasonable constants (`SLEV`, `CLRG`) in `Pacs008Mapper.java` rather than left to guesswork from a non-existent source field.

3. **No BCE field-level specification is publicly available** (which SPI1 field maps exactly to which ISO 20022 element) at the time this PoC was built. The field mapping (`Pacs008Mapper.java`) was designed **by analogy** with Colombia's immediate payments mechanism (MOL / Bre-B), which has a public technical specification, adapted to Ecuadorian fields and regulations (LAFT, USD 15,000 limit). **This is a reasoned architectural reference, not the official BCE specification.**

4. **Identification type codes (`CI`, `CE`, `RUC`, `PASS`)** come from the public technical documentation of Local Payment, a payment processor operating in Ecuador — used as a verified substitute for the official SRI "Table No. 2 - Identification Type" catalog (part of the ROTEF Annex technical spec), whose catalog values could not be obtained from any publicly accessible source (only confirmed that the catalog exists). If the official catalog is obtained, these codes should be replaced.

5. `SwitchGatewaySimulator` is a local deterministic simulator for the demo — **it does not connect to any real BCE system.**

6. **Security**: OAuth 2.0 (client_credentials) + JWT and mTLS ARE implemented in code (see Security section above), but as **simulations of the mechanics**, not production-grade components — no real Authorization Server, no JWKS/key rotation, and mTLS certificates are self-signed and for local demo only. The 10-year LAFT retention requirement is documented as a requirement but not implemented as a real retention policy (H2 is in-memory and lost on shutdown).

## What this project is NOT

- Not a certified or audited implementation by the BCE.
- Does not use real data from any financial institution (all sample data in `examples/` is fictional).
- Does not implement production-grade authentication, encryption in transit, or other production security controls — these are documented, not coded.

## Repository structure

```
.github/workflows/ci.yml   CI pipeline (mvn verify on every push/PR)
.gitignore
LICENSE                     MIT, with scope note (not production software)
pom.xml
src/main/java/com/stan/poc/spi2iso/
  model/         Spi1Record (SPI1 record POJO)
  jaxb/          XmlUtil (marshalling + date/time conversion for the generated binding)
  jaxb/generated/pacs008, .../pacs002   xjc output (not versioned — see pom.xml / xsd/ below)
  parser/        Spi1Parser
  generator/     Spi1FileGenerator
  mapping/       Pacs008Mapper
  validation/    Pacs008Validator, ValidationResult
  gateway/       SwitchGatewaySimulator
  persistence/   TransactionLog (JPA entity), TransactionLogRepository
  service/       Spi1ToIso20022Service (pipeline orchestration)
  controller/bian/ PaymentExecutionController, ClearingAndSettlementController (REST API with BIAN nomenclature)
  config/        OpenApiConfig (Swagger / OpenAPI 3 metadata)
  security/      SecurityConfig, JwtAuthenticationFilter, ClientRegistry, TokenController
  security/jwt/  JwtService (demo JWT issuance/validation)
  security/mask/ LaftMaskingUtil (sensitive data masking in logs)
src/main/resources/
  application.properties        main configuration (includes demo secrets)
  application-mtls.properties   optional mTLS profile
  certs/                         demo keystores (generated by script, not versioned)
  xsd/pacs008/, xsd/pacs002/     official ISO 20022 XSDs (source of truth for xjc)
scripts/generar-certificados-mtls.sh   generates self-signed demo certificates
src/test/java/...  PipelineTest (full pipeline) + JwtServiceTest (token issuance/validation)
examples/spi1-demo.txt   sample SPI1 file (3 scenarios)
docs/governance.md   Layer 5 — TOGAF ADM mapping, migration roadmap, risk matrix
```

## Author

Stan — built as portfolio evidence for the Integration Architect role application, Banco Pichincha (Ecuador).
