# PoC: Motor de transformación SPI1 → ISO 20022 (Ecuador)

[![CI](https://github.com/Stan017/spi1-iso20022-poc/actions/workflows/ci.yml/badge.svg)](https://github.com/Stan017/spi1-iso20022-poc/actions/workflows/ci.yml)


Proyecto de portafolio técnico, construido para demostrar competencias de
**arquitectura de integración** (TOGAF 9.2, BIAN, ITIL, COBIT) aplicadas a un
problema regulatorio real y vigente del sistema financiero ecuatoriano: la
migración de los archivos planos SPI1 del Banco Central del Ecuador (BCE) hacia
mensajería ISO 20022.

> Este es un **proof of concept de portafolio**, no software de producción ni una
> implementación oficial o certificada por el Banco Central del Ecuador. Ver la
> sección "Alcance, fuentes y supuestos" antes de citarlo como evidencia técnica.

## El problema de negocio

- **Resolución BCE-GG-007-2026** (marzo 2026): exige ISO 20022 como estándar de
  mensajería para pagos en tiempo real hasta USD 15,000. Plazo para la mensajería
  interna entre participantes de la red de pagos: **30 de noviembre de 2026**.
- **Circular BCE-GMPSN-2026-008-C / 013-C**: el formato de archivo SPI1 (v2) agrega
  un campo obligatorio nuevo — cédula/RUC/pasaporte del ordenante — por el Art. 52
  de la Ley de Prevención de Lavado de Activos (LAFT). Vigente desde el **6 de
  julio de 2026**.

Las instituciones financieras ecuatorianas deben pasar de un modelo de **archivos
planos por lotes, en 3 cortes diarios**, a mensajería **ISO 20022 en tiempo real**
— un cambio arquitectónico, no solo de formato.

## Qué hace este PoC

Implementa el motor de transformación de punta a punta:

```
archivo SPI1 (plano) → Spi1Parser → Pacs008Validator → Pacs008Mapper
   → pacs.008.001.08 (XML real, vía JAXB) → SwitchGatewaySimulator
   → pacs.002.001.10 (XML real, vía JAXB) → TransactionLog (H2, con trazabilidad T510/T515/T520/T530)
```

Expuesto también como API REST (Spring Boot) y con 3 escenarios de demo
(aceptado / rechazado / timeout) en `examples/spi1-demo.txt`.

## Alineación con BIAN

BIAN (Banking Industry Architecture Network) organiza la banca en **Service
Domains**. Este PoC mapea su pipeline a los dos dominios relevantes para una
transferencia interbancaria, y lo refleja directamente en el código — no solo en el
documento de diseño:

| Service Domain BIAN | Rol en este PoC | Código |
|---|---|---|
| **Payment Execution** | Recibe la instrucción SPI1, la valida, la transforma a ISO 20022 y gestiona su ciclo de vida hasta entregarla a liquidación. | `PaymentExecutionController`, `Spi1ToIso20022Service` |
| **Clearing and Settlement** | Liquida o rechaza la instrucción ya transformada y devuelve el resultado (pacs.002). | `ClearingAndSettlementController`, `SwitchGatewaySimulator` |

**Control Record**: BIAN define, para cada Service Domain, un Control Record — el
objeto de negocio central cuyo ciclo de vida se rastrea. En este PoC es
`TransactionLog`, identificado por su `numeroInstruccion` (expuesto en la API como
`crId`, Control Record Id). Por simplicidad de PoC, Payment Execution y Clearing and
Settlement comparten el mismo Control Record en vez de tener cada uno el suyo
correlacionado — en una arquitectura real serían instancias separadas.

**Behavior Qualifiers** (las acciones estándar que BIAN define sobre un Control
Record) cubiertos por las rutas REST:

| Behavior Qualifier | Implementado | Dónde |
|---|---|---|
| **Initiate** | Sí | `POST /payment-execution/payment-instructions/initiate` (+ variante `initiate-batch`) |
| **Retrieve** | Sí | `GET .../retrieve` en ambos dominios |
| Update | No | fuera de alcance — modificar una instrucción ya iniciada |
| Control | No | fuera de alcance — suspender/reanudar/cancelar |
| Exchange / Notify | No | fuera de alcance — eventos asíncronos entre Service Domains |

Esto es, honestamente, un mapeo **arquitectónico/conceptual**: cubre los dos
dominios correctos, el patrón de interacción correcto (Initiate en Payment
Execution → entrega a Clearing and Settlement → Retrieve del resultado), y usa la
nomenclatura real de BIAN en las rutas — pero no implementa el modelo de datos
formal completo de BIAN (Service Operations con su esquema XSD/OpenAPI oficial,
Behavior Qualifiers adicionales, ni Service Domains separados con su propia
persistencia). Para una entrevista técnica, esto demuestra que se entiende el
framework y se sabe aplicarlo, no que se implementó el estándar BIAN completo.

## Cómo correrlo

```bash
mvn spring-boot:run -Ddemo.file=examples/spi1-demo.txt
```

Esto procesa el archivo de ejemplo al arrancar (verás el resultado de las 3
transacciones en consola) y deja la API levantada en `http://localhost:8080`.

Para correr solo las pruebas:

```bash
mvn test
```

### Integración continua

`.github/workflows/ci.yml` corre `mvn verify` (compilación + pruebas) en cada
push/PR a `main` vía GitHub Actions. Esto importa especialmente para este
proyecto: se construyó en un entorno sin salida de red hacia Maven Central, así
que nunca pudo compilarse "en caliente" durante el desarrollo — este pipeline es
la primera verificación real, y la que debe quedar en verde antes de presentarlo
como evidencia técnica.

### Endpoints

Las rutas siguen la nomenclatura de **Service Domain** y **Behavior Qualifier** de
BIAN (ver la sección "Alineación con BIAN" más abajo) en vez de nombres genéricos:

| Método | Ruta | Service Domain BIAN | Behavior Qualifier |
|---|---|---|---|
| POST | `/payment-execution/payment-instructions/initiate` (body: texto plano, una línea SPI1) | Payment Execution | Initiate |
| POST | `/payment-execution/payment-instructions/initiate-batch?ruta=examples/spi1-demo.txt` | Payment Execution | Initiate (variante batch, conveniencia de demo) |
| GET | `/payment-execution/payment-instructions/{crId}/retrieve` | Payment Execution | Retrieve |
| GET | `/payment-execution/payment-instructions/retrieve` | Payment Execution | Retrieve (todas) |
| GET | `/clearing-and-settlement/settlement-status/{crId}/retrieve` | Clearing and Settlement | Retrieve |
| — | `/h2-console` (JDBC URL `jdbc:h2:mem:spi1poc`, user `sa`) | — | consola H2 para inspeccionar el log |

`{crId}` es el Control Record Id — en este PoC, el `numeroInstruccion` de la
instrucción SPI1 (ver `TransactionLog.java` y la sección BIAN abajo).

Todas requieren el header `Authorization: Bearer <token>` (ver la sección
Seguridad) excepto `/oauth/token` y `/h2-console`.

### Los 3 escenarios de la demo

1. **`INS-0001`** — transacción válida → el switch simulado responde `ACSC` (liquidada).
2. **`INS-0002`** — monto `999.99` (regla de demo) → el switch simulado responde `RJCT`,
   motivo `AM02`, con el `pacs.002` correspondiente.
3. **`INS-0003`** — referencia con la palabra `TIMEOUT` → el switch simulado no responde,
   para demostrar el manejo de ese caso en el log de trazabilidad.

## Seguridad (Capa 4)

El PoC implementa dos mecanismos de seguridad, alineados con el diseño documentado:

### 1. OAuth 2.0 (flujo `client_credentials`) + JWT

Las rutas de la API quedan protegidas con Spring Security. Para llamarlas hay que
pedir primero un token:

```bash
curl -s -X POST http://localhost:8080/oauth/token \
  -d "grant_type=client_credentials" \
  -d "client_id=banco-pichincha-poc-client" \
  -d "client_secret=demo-secret-cambiar-en-uso-real" \
  -d "scope=transferencias:read transferencias:write"
```

Devuelve un `access_token` (JWT) que se usa como `Authorization: Bearer <token>` en
las llamadas a `/api/**`:

```bash
TOKEN="<pegar el access_token de arriba>"

curl -s http://localhost:8080/payment-execution/payment-instructions/retrieve \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://localhost:8080/payment-execution/payment-instructions/initiate-batch?ruta=examples/spi1-demo.txt" \
  -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8080/clearing-and-settlement/settlement-status/INS-0001/retrieve \
  -H "Authorization: Bearer $TOKEN"
```

- Las rutas `GET .../retrieve` exigen el scope `transferencias:read`.
- Las rutas `POST .../initiate*` exigen el scope `transferencias:write`.
- Sin token, o con un token inválido/expirado/con el scope equivocado → `401`/`403`.

**Límite honesto**: `TokenController` y `JwtService` simulan la *mecánica* de un
Authorization Server OAuth2 (emisión de `access_token` con scopes y expiración,
validación Bearer), implementada a mano con JDK + Jackson — no hay JWKS, no hay
rotación de llaves, la firma es simétrica (HS256) y el secreto vive en
`application.properties`. En un entorno real del Banco, el Authorization Server
sería un componente dedicado (Keycloak, Okta, IdP corporativo) con firma asimétrica
y rotación de llaves — ver el comentario en `JwtService.java` para el detalle.

### 2. mTLS (perfil opcional `mtls`)

Para probar la autenticación mutua por certificado (canal API Gateway ↔ switch del
BCE, como está documentado en el diseño):

```bash
chmod +x scripts/generar-certificados-mtls.sh
./scripts/generar-certificados-mtls.sh   # genera certificados autofirmados de demo
mvn spring-boot:run -Dspring-boot.run.profiles=mtls
```

La API queda en `https://localhost:8443`, exigiendo que el cliente presente el
certificado generado (`certs/cliente.p12`). No se activa por defecto porque
requiere generar esos certificados primero. Ver `application-mtls.properties`
para el detalle y las limitaciones (mTLS aquí es solo transporte; la identificación
de la operación sigue viviendo en el token OAuth2/JWT).

### 3. Enmascaramiento de datos LAFT

`LaftMaskingUtil` centraliza el enmascaramiento del número de identificación/cuenta
que se imprime en los logs de demo (el dato completo sí se persiste en
`TransactionLog`, igual que en un sistema real sujeto a retención de 10 años por el
Art. 52 LAFT — el enmascaramiento es solo para lo que se muestra en consola).

### Nota sobre el arranque

Al levantar la app sin este perfil de seguridad personalizado completamente
"limpio", Spring Boot puede imprimir un warning de una contraseña generada para un
usuario `user` por defecto (`UserDetailsServiceAutoConfiguration`). Es ruido inocuo:
este PoC no usa ese usuario ni `httpBasic`/`formLogin` — toda la autenticación real
pasa por el flujo OAuth2/JWT descrito arriba.

## Decisiones de stack (confirmadas)

| Decisión | Valor |
|---|---|
| Lenguaje / framework | Java 17 + Spring Boot 3.2 + Maven |
| Formato de mensaje | XML real, binding con JAXB (Jakarta XML Bind) |
| Persistencia | H2 embebido en memoria, JPA/Hibernate |
| Vocabulario de patrones | Enterprise Integration Patterns (channel, transformer, router, validator) |

## Alcance, fuentes y supuestos (léase antes de presentarlo)

Este PoC fue construido siendo deliberadamente honesto sobre sus límites, porque
eso es parte de lo que lo hace defendible frente a un entrevistador técnico:

1. **El layout exacto del archivo SPI1 real no es público.** El BCE no publica en
   una fuente abierta el formato de posiciones/anchos de campo que usa hoy la banca
   ecuatoriana. Este proyecto define un **formato delimitado razonable** (ver
   `Spi1Parser.java`) con los mismos campos de negocio exigidos por la normativa
   citada arriba, para poder demostrar el motor de transformación de punta a punta.

2. **Las clases JAXB de pacs.008/pacs.002 no se generaron desde el XSD oficial de
   iso20022.org.** El entorno de desarrollo usado no tiene salida de red hacia ese
   dominio. Las clases en `jaxb/Pacs008Document.java` y `jaxb/Pacs002Document.java`
   fueron **escritas a mano**, reproduciendo la jerarquía y los nombres de elementos
   reales de esos mensajes (`GrpHdr`, `CdtTrfTxInf`, `IntrBkSttlmAmt`, `Dbtr`,
   `CdtrAgt`, `TxSts`, etc.) según la documentación pública de ISO 20022. **Antes de
   usar esto en un contexto real, se debe regenerar el binding con `xjc` a partir
   del XSD oficial.**

3. **No existe una especificación pública del BCE a nivel de campo** (qué campo
   SPI1 va exactamente a qué elemento ISO 20022) al momento de construir este PoC.
   El mapeo de campos (`Pacs008Mapper.java`) se diseñó **por analogía** con el
   mecanismo operativo de pagos inmediatos de Colombia (MOL / Bre-B), que sí tiene
   especificación técnica pública, adaptado a los campos y a la normativa
   ecuatoriana (LAFT, límite de USD 15,000). **Esto es una referencia arquitectónica
   razonada, no la especificación oficial del BCE.**

4. **Los códigos de tipo de identificación (`CI`, `CE`, `RUC`, `PASS`)** provienen
   de la documentación técnica pública de Local Payment, un procesador de pagos que
   opera en Ecuador — usados como sustituto verificado de la "Tabla No. 2 Tipo de
   identificación" oficial del SRI (parte de la ficha técnica del Anexo ROTEF), cuyos
   valores de catálogo no se pudieron obtener de una fuente pública accesible (solo
   se confirmó que ese catálogo existe). Si se obtiene el catálogo oficial, estos
   códigos deben reemplazarse.

5. El `SwitchGatewaySimulator` es un simulador local determinista para la demo —
   **no se conecta a ningún sistema real del BCE.**

6. **Seguridad**: OAuth 2.0 (client_credentials) + JWT y mTLS SÍ están implementados
   en código (ver la sección "Seguridad" arriba), pero como **simulaciones de la
   mecánica**, no como componentes de grado productivo — no hay Authorization
   Server real, no hay JWKS/rotación de llaves, y los certificados mTLS son
   autofirmados y solo para la demo local. La retención de 10 años por LAFT está
   documentada como requisito pero no implementada como política de retención real
   (el H2 es en memoria y se pierde al apagar la app).

## Qué NO es este proyecto

- No es una implementación certificada ni auditada por el BCE.
- No usa datos reales de ninguna institución financiera (todos los datos de
  ejemplo en `examples/` son ficticios).
- No implementa autenticación, cifrado en tránsito, ni los demás controles de
  seguridad de producción — están documentados, no codificados.

## Estructura del repositorio

```
.github/workflows/ci.yml   pipeline de CI (mvn verify en cada push/PR)
.gitignore
LICENSE                     MIT, con nota de alcance (no es software de producción)
pom.xml
src/main/java/com/stan/poc/spi2iso/
  model/         Spi1Record (POJO del registro SPI1)
  jaxb/          Pacs008Document, Pacs002Document (binding JAXB hecho a mano) + XmlUtil
  parser/        Spi1Parser
  generator/     Spi1FileGenerator
  mapping/       Pacs008Mapper
  validation/    Pacs008Validator, ValidationResult
  gateway/       SwitchGatewaySimulator
  persistence/   TransactionLog (entidad JPA), TransactionLogRepository
  service/       Spi1ToIso20022Service (orquestación del pipeline)
  controller/bian/ PaymentExecutionController, ClearingAndSettlementController (API REST con nomenclatura BIAN)
  security/      SecurityConfig, JwtAuthenticationFilter, ClientRegistry, TokenController
  security/jwt/  JwtService (emisión/validación de JWT de demo)
  security/mask/ LaftMaskingUtil (enmascaramiento de datos sensibles en logs)
src/main/resources/
  application.properties        configuración principal (incluye secretos de demo)
  application-mtls.properties   perfil opcional de mTLS
  certs/                         keystores de demo (generados por el script, no versionados)
scripts/generar-certificados-mtls.sh   genera los certificados autofirmados de demo
src/test/java/...  PipelineTest (pipeline completo) + JwtServiceTest (emisión/validación de tokens)
examples/spi1-demo.txt   archivo SPI1 de ejemplo (3 escenarios)
```

## Autor

Stan — construido como evidencia de portafolio para la postulación al rol de
Arquitecto de Integración, Banco Pichincha (Ecuador).
