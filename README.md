# Internet Comparison

> **Status:** WIP / Prototype – **nicht produktionsreif**.  
> Fokus: funktionales Grundgerüst & UX (schnelles erstes Ergebnis, danach Nachladen).

## Scope
- Vergleicht Internet-Tarife verschiedener Provider für eine Adresse.
- Spring Boot 3 (Java 17), Thymeleaf + Tailwind, parallelisierte Provider-Abfragen.
- Deployment: z. B. Railway / beliebige Container-Runtime.

## Nicht abgedeckt (noch)
- Resilienz: Timeouts, Retries, Circuit-Breaker, Bulkheads.
- Effiziente Ergebniszustellung (SSE/WebSocket statt Polling).
- Caching mit TTL + Adress-Normalisierung.
- Rate Limiting, Captcha, Security-Headers, CSRF-Schutz.
- Telemetrie (structured logging, Metriken, Tracing).

## Roadmap (Next)
1. **Resilience4j** für alle Provider-Clients (Timeout, Retry, Circuit-Breaker).
2. **SSE-Endpoint** (`text/event-stream`) zum Streamen eingehender Angebote.
3. **Caffeine Cache** (24h TTL) mit normalisiertem Address-Key.
4. **Observability**: Micrometer (Timer/Counter pro Provider), Request-ID.
5. **Input-Validierung** (Bean Validation) + **Rate Limiting** (Bucket4j).
6. **Dockerize** + Health/Readiness + minimaler CI-Check (`mvn -B verify`).

## Quickstart (dev)
```bash
# Build & run
./mvnw spring-boot:run

# Prod-ähnlich mit Profil:
./mvnw -Dspring-boot.run.profiles=prod spring-boot:run



 **Tech-Stack**

Java 17

Spring Boot 3

Maven

Thymeleaf

REST APIs

Railway (Cloud-Deployment)

GitHub (CI / Versionskontrolle)

 **Aufbau & Architektur**

__Frontend__

basiert auf Thymeleaf & TailwindCSS

Formular für Adresseingabe (compare.html)

Ergebnisse mit Endlos-Scrollen (result.html)

__Backend__

ComparisonController verarbeitet Eingaben und liefert Batches von Angeboten

ComparisonServiceImpl ruft parallel Angebote von 5 Provider-Clients ab:

ByteMeClient

PingPerfectClient

WebWunderClient

VerbynDichClient

ServusSpeedClient

Asynchrone Architektur

Nach dem ersten synchronen Abruf wird das Laden im Hintergrund gestartet

Ergebnisse werden gecached (pro Adresse = CacheKey)

Abgerufene Angebote werden im Hintergrund gespeichert und per Batch ausgeliefert

Endlos-Scroll-Mechanismus

GET /compare/batch liefert stufenweise Angebote (5 pro Aufruf)

Frontend fragt alle 3 Sekunden nach neuen Angeboten, bis keine mehr da sind

   **Deployment auf Railway**

Voraussetzungen

.gitignore enthält alle IDE-Dateien & application.yml

application.yml wird über Railway-"Secrets" ersetzt

Schritte

Projekt auf GitHub pushen

Railway verbinden mit GitHub-Repo

Im Railway-Dashboard:

Build Command: mvn clean package -DskipTests

Start Command: java -jar target/internet-comparison-0.0.1-SNAPSHOT.jar

Secrets im Railway-Tab Variables eintragen

Beispiel .gitignore

/target
/.idea
/.vscode
*.iml
application.yml
keystore.p12

  **Status**

  Paralleles Laden funktioniert

  Nachladen per Scroll / Timer aktiv

  Railway Deployment getestet

Bei Fragen oder Fehlern gerne Issue auf GitHub eröffnen 

