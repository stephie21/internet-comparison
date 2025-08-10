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




