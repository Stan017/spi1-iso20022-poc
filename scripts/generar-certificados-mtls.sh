#!/usr/bin/env bash
#
# Genera un juego de certificados AUTOFIRMADOS de demo para probar el perfil "mtls"
# de este PoC (ver application-mtls.properties). SOLO para uso local de demo - un
# certificado autofirmado NUNCA debe usarse en un entorno real; ahi los emitiria una
# CA propia del Banco o una CA publica.
#
# Genera:
#   certs/servidor.p12     - keystore del servidor (clave privada + certificado)
#   certs/cliente.p12      - keystore del cliente, para presentar al conectarse
#   certs/truststore.p12   - truststore del servidor, confia en el certificado del cliente
#
# Uso:
#   chmod +x scripts/generar-certificados-mtls.sh
#   ./scripts/generar-certificados-mtls.sh
#
# Requiere: keytool (incluido en el JDK).

set -euo pipefail

CERTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/src/main/resources/certs"
PASSWORD="cambia-esta-clave-de-demo"
DIAS_VALIDEZ=365

mkdir -p "$CERTS_DIR"
cd "$CERTS_DIR"

echo "==> Generando keystore del servidor (servidor.p12)"
keytool -genkeypair \
  -alias servidor \
  -keyalg RSA -keysize 2048 \
  -validity "$DIAS_VALIDEZ" \
  -keystore servidor.p12 -storetype PKCS12 -storepass "$PASSWORD" \
  -dname "CN=localhost, OU=PoC Integracion, O=Portafolio Stan, L=Milagro, ST=Guayas, C=EC"

echo "==> Generando keystore del cliente (cliente.p12)"
keytool -genkeypair \
  -alias cliente \
  -keyalg RSA -keysize 2048 \
  -validity "$DIAS_VALIDEZ" \
  -keystore cliente.p12 -storetype PKCS12 -storepass "$PASSWORD" \
  -dname "CN=cliente-poc-switch, OU=PoC Integracion, O=Portafolio Stan, L=Milagro, ST=Guayas, C=EC"

echo "==> Exportando certificado publico del cliente"
keytool -exportcert \
  -alias cliente -keystore cliente.p12 -storetype PKCS12 -storepass "$PASSWORD" \
  -file cliente.crt -rfc

echo "==> Construyendo truststore del servidor con el certificado del cliente"
keytool -importcert \
  -alias cliente -file cliente.crt \
  -keystore truststore.p12 -storetype PKCS12 -storepass "$PASSWORD" \
  -noprompt

rm -f cliente.crt

echo ""
echo "Listo. Archivos generados en: $CERTS_DIR"
echo "Recuerda actualizar la clave en application-mtls.properties si cambiaste \$PASSWORD."
echo "Para probar: mvn spring-boot:run -Dspring-boot.run.profiles=mtls"
echo "Luego llama a la API con el certificado de cliente, por ejemplo con curl:"
echo "  curl --cacert <tu CA o --insecure para demo> \\"
echo "       --cert-type P12 --cert certs/cliente.p12:$PASSWORD \\"
echo "       https://localhost:8443/api/transacciones"
