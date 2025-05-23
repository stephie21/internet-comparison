#!/bin/sh

echo "💡 Erzeuge keystore.p12 aus Umgebungsvariable ..."
mkdir -p /app/resources
echo "$KEYSTORE_BASE64" | base64 -d > /app/resources/keystore.p12

echo "🚀 Starte Spring Boot App ..."
exec java -jar target/*.jar
