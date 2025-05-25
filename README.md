Internet Comparison App

Dies ist ein Spring Boot-Projekt, das Internetangebote verschiedener Anbieter vergleicht. Die Anwendung ruft parallel Angebote ab und zeigt sie dem Nutzer auf einer Website, während im Hintergrund weitere Angebote nachgeladen werden.

 Ziel

Ziel ist es, dem Nutzer eine schnelle Übersicht über Internetangebote an einer bestimmten Adresse zu geben. Es wird sofort ein erstes Angebot angezeigt, weitere Angebote erscheinen nach und nach (Endlos-Scrollen).

 Tech-Stack

Java 17

Spring Boot 3

Maven

Thymeleaf

REST APIs

Railway (Cloud-Deployment)

GitHub (CI / Versionskontrolle)

 Aufbau & Architektur

Frontend

basiert auf Thymeleaf & TailwindCSS

Formular für Adresseingabe (compare.html)

Ergebnisse mit Endlos-Scrollen (result.html)

Backend

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

   Deployment auf Railway

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

  Status

  Paralleles Laden funktioniert

  Nachladen per Scroll / Timer aktiv

  Railway Deployment getestet

Bei Fragen oder Fehlern gerne Issue auf GitHub eröffnen 

