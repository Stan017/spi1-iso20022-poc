# Instrucciones de contexto para Claude Code

Este archivo es el handoff de este proyecto desde una sesión anterior de Claude
(claude.ai) hacia Claude Code. Léelo completo antes de tocar nada — te da el
contexto que la sesión anterior tenía y que tú no tienes todavía.

## Qué es este proyecto

PoC (proof of concept) de portafolio técnico para **Stan**, quien lo está
construyendo para sustentar una postulación al rol de **Arquitecto de Integración
en Banco Pichincha (Ecuador)**. No es un encargo de cliente ni software real de
producción — es evidencia técnica para una entrevista.

El PoC resuelve (de forma honesta, no inventada) un problema regulatorio real y
vigente: la migración de los archivos planos **SPI1** del Banco Central del Ecuador
(BCE) hacia mensajería **ISO 20022** (pacs.008/pacs.002), exigida por:
- Resolución BCE-GG-007-2026 (plazo: 30-nov-2026)
- Circular BCE-GMPSN-2026-008-C/013-C (campo LAFT obligatorio desde 6-jul-2026)

**Lee `README.md` primero** — tiene el detalle completo (problema de negocio,
arquitectura, fuentes, supuestos, límites honestos, cómo correrlo). Este archivo
(`CLAUDE.md`) es un resumen orientado a continuar el trabajo, no un duplicado.

## Lo más importante que debes hacer primero

**Este proyecto NUNCA fue compilado ni probado de verdad.** Se construyó en un
sandbox (claude.ai, no Claude Code) cuya salida de red estaba restringida a
npm/PyPI/GitHub — **sin acceso a Maven Central**. Todo el código fue revisado a
mano, con mucho cuidado, pero no hay ninguna garantía de que compile a la primera.

Por eso, tu primera acción en este proyecto, antes de construir nada nuevo, debe
ser:

```bash
mvn verify
```

y arreglar lo que falle. Es muy probable que haya 1-2 errores menores de
compilación (nombres de métodos, imports, tipos) porque se escribió sin feedback
del compilador. Trátalo como una revisión de código real, no como código
confiable que "ya funciona".

También existe `.github/workflows/ci.yml` (GitHub Actions) que corre `mvn verify`
en cada push — pero corre eso también localmente primero, es más rápido iterar.

## Stack y decisiones ya confirmadas con Stan (no las cuestiones sin preguntarle)

- **Lenguaje/framework**: Java 17 + Spring Boot 3.2 + Maven — confirmado, no Kotlin/Node.
- **Formato de mensaje**: XML real con JAXB (binding escrito a mano, no generado
  desde el XSD oficial de iso20022.org porque ese dominio no era alcanzable desde
  el sandbox anterior — ver limitación en README). Si ahora SÍ tienes acceso a
  internet normal, considera ofrecerle a Stan regenerar el binding con `xjc` desde
  el XSD real descargado de iso20022.org — sería una mejora real de fidelidad.
- **Persistencia**: H2 embebido en memoria (JPA/Hibernate) — confirmado.
- **Seguridad**: OAuth2 client_credentials + JWT (implementación propia, HMAC-SHA256,
  sin librería externa) + mTLS opcional (perfil `mtls`, certificados autofirmados
  generados por `scripts/generar-certificados-mtls.sh`).
- **Framework de arquitectura**: BIAN — las rutas REST siguen la nomenclatura de
  Service Domain/Behavior Qualifier de BIAN (`/payment-execution/payment-instructions/...`,
  `/clearing-and-settlement/settlement-status/...`). Ver sección "Alineación con
  BIAN" en el README para el detalle y los límites de ese mapeo.

## Qué está construido vs. qué es solo diseño/documentación

Construido como código (en este repo):
- Capa 3 (arquitectura): parser SPI1, modelo JAXB pacs.008/pacs.002, mapper,
  validador, switch simulado, persistencia H2 con trazabilidad T510/T515/T520/T530.
- Capa 4 (seguridad): OAuth2/JWT, mTLS opcional, enmascaramiento LAFT.
- Alineación BIAN en las rutas REST y en el Control Record (`TransactionLog`).
- CI (GitHub Actions), `.gitignore`, `LICENSE` (MIT).

Solo documentado (en un Claude Docs — Stan tiene el link, pregúntale si lo necesitas,
no está en este repo):
- Capa 1-2: alcance, criterios de éxito, mapeo de campos SPI1→pacs.008.
- Capa 6 (empaquetado): esto ya se está materializando en este propio repo.

Ya materializado en este repo:
- Capa 5 (gobierno): `docs/governance.md` — mapeo TOGAF ADM, roadmap de migración
  por hito regulatorio, matriz de riesgo. Escrito en inglés para ser consistente
  con el README (también ya traducido). Si Stan pide seguir iterando sobre
  gobierno, el contenido vive ahí — no vuelvas a copiarlo desde el Claude Doc
  original sin confirmar con él primero cuál es la fuente de verdad.
- Swagger/OpenAPI: `OpenApiConfig` + anotaciones `@Operation`/`@Tag` en los
  3 controllers, Swagger UI en `/swagger-ui.html`, probado end-to-end.
- CI en GitHub Actions: corriendo en verde en la rama `main` (el repo real está
  en https://github.com/Stan017/spi1-iso20022-poc).

## Limitaciones ya conocidas y comunicadas a Stan (no las presentes como nuevas)

1. El layout exacto del archivo SPI1 real no es público — se definió un formato
   delimitado propio con los mismos campos de negocio.
2. Las clases JAXB no vienen de un XSD oficial descargado (ver arriba).
3. El mapeo de campos SPI1→ISO20022 es por analogía con el modelo MOL/Bre-B de
   Colombia (público), no la especificación oficial del BCE (que no existe pública).
4. Los códigos de tipo de identificación (CI/CE/RUC/PASS) vienen de un procesador
   de pagos real (Local Payment), como sustituto del catálogo oficial SRI (Tabla
   No. 2, Anexo ROTEF) cuyos valores no se pudieron obtener.
5. El `SwitchGatewaySimulator` es 100% local/determinista, no conecta a nada real.
6. OAuth2/JWT y mTLS son simulaciones de la mecánica, no componentes de grado
   productivo (sin JWKS, sin rotación de llaves, certificados autofirmados).

Todo esto YA está en el README con el detalle completo — mantenlo ahí si lo tocas,
no lo borres ni lo suavices: la honestidad sobre estos límites es parte de lo que
hace al proyecto defendible en una entrevista técnica.

## Pendientes conocidos (en orden sugerido)

Ya resueltos (no los vuelvas a proponer como pendientes): `mvn verify` corre
limpio, Swagger/OpenAPI está implementado y probado, el repo está en GitHub
(`Stan017/spi1-iso20022-poc`, rama `main`, CI en verde), el README está en
inglés, y la Capa 5 (gobierno) ya vive en `docs/governance.md`.

Lo único que queda, y es opcional:

1. Si tienes acceso real a internet, considera regenerar el binding JAXB
   (`Pacs008Document`/`Pacs002Document`) desde el XSD oficial de iso20022.org
   con `xjc` (ver nota de stack arriba) y confirmarle a Stan si quiere ese
   nivel de fidelidad — hoy el binding está escrito a mano.

## Cómo trabajar con Stan en esto

Stan es trader y arquitecto de software (background Java/móvil), técnicamente
fluido — no necesitas explicarle conceptos básicos de arquitectura o de Spring
Boot. Sí aprecia que seas honesto sobre qué es simulado/simplificado vs. real,
porque el objetivo es que el proyecto resista preguntas de un entrevistador
técnico. No le ocultes ni le edulcores limitaciones del código para que "se vea
mejor" — es lo opuesto a lo que quiere.
