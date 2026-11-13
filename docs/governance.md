# Architecture governance (Layer 5)

This document translates the PoC into the governance frameworks expected from
an Integration Architect role (TOGAF 9.2, BIAN) and lays out the project's
risk matrix. It complements `README.md` — technical detail (stack, endpoints,
limitations) lives there; this file is only about how the project is governed.

## TOGAF ADM mapping

| ADM Phase | Application in this project |
|---|---|
| **A. Architecture Vision** | Gap identified: batch model (SPI1 flat files, 3 daily settlement windows) vs. real-time messaging (ISO 20022), driven by a BCE regulatory mandate (Resolution BCE-GG-007-2026). |
| **B. Business Architecture** | Interbank payment process, actors (payer, payee, BCE), AML obligation (Art. 52 LAFT, mandatory payer identification field). |
| **C. Information Systems Architecture** | SPI1 → pacs.008 data model and field mapping (see README, "Scope, sources and assumptions"); transformation engine components (`parser/`, `mapping/`, `validation/`). |
| **D. Technology Architecture** | PoC stack (Java 17 + Spring Boot + JAXB + H2), security mechanisms (`security/`: OAuth2/JWT, optional mTLS, LAFT masking), and the API contract surfaced via OpenAPI/Swagger (`config/OpenApiConfig`). |
| **E. Opportunities & Solutions** | This PoC itself: a scoped proof of concept for the transformation layer, not an end-to-end solution for the whole migration program. |
| **F. Migration Planning** | Roadmap phased by regulatory milestones (see below). |

## BIAN mapping

The functional domain maps to **Payment Execution** within BIAN's Business
Object Model — specifically the **Clearing and Settlement** interaction
pattern, which models the pacs.008/pacs.002 exchange between a participant
(the originating bank) and a central switch (the BCE).

This mapping is not only conceptual: it is reflected directly in the code —
REST routes follow BIAN's Service Domain/Behavior Qualifier nomenclature
(`/payment-execution/payment-instructions/...`,
`/clearing-and-settlement/settlement-status/...`), each domain's Control
Record is the `TransactionLog` entity, and the full contract is documented
and explorable via Swagger UI (`/swagger-ui.html`). See the "BIAN alignment"
section of `README.md` for the complete detail, including which Behavior
Qualifiers are implemented (Initiate, Retrieve) and which are not (Update,
Control, Exchange/Notify).

## Migration roadmap (phases, regulatory reference)

| Phase | Milestone | BCE deadline |
|---|---|---|
| 1 | Add the payer identification field to SPI1 v2 | July 6, 2026 |
| 2 | Implement the SPI1 → ISO 20022 transformation layer (this PoC covers this phase — see README for its real vs. conceptual scope) | — |
| 3 | ISO 20022 messaging compliance for internal payment-network transactions | November 30, 2026 |
| 4 | Full onboarding to the Instant Payments Network (RPI), staged by asset size | Up to 9 months from the regulation, depending on tier |

## Risk matrix

| Risk | Category | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| Incomplete or incorrect field mapping against the BCE's real (non-public) specification | Technical | High | Medium | Assumption explicitly stated (README, "Scope, sources and assumptions"); to be validated against any official BCE documentation published later. |
| Sensitive data (national ID / RUC) leaking into demo logs | Compliance | Medium | High | Implemented: `LaftMaskingUtil` masks the identification number everywhere it is printed to logs/console; the full value is only persisted in `TransactionLog` (in-memory H2), never using real personal data. |
| PoC scope creep beyond what Layer 1 defined | Project | High | Medium | Check new functionality against the README's "What this project is NOT" list before adding it. |
| Unavailability of an official BCE specification leaves the PoC misaligned once one is published | Technical | Medium | Medium | Modular design: the mapping layer (`mapping/Pacs008Mapper`) can be adjusted without rebuilding the rest of the system. |
| Code had never been compiled in the environment where it was originally written | Technical | Resolved | High (if unresolved) | CI pipeline (`.github/workflows/ci.yml`, `mvn verify` on every push/PR) is in place and green on `main` — see README and `CLAUDE.md`. |
