# SPS Monitoring & Control System

Dieses Projekt ist ein vollständiges **IoT-System** zur Überwachung und Steuerung von SPSen (Wago 750 & Siemens S7-1500). Es umfasst MQTT-Integration, Datenpersistenz in MongoDB, ein REST-API-Backend und ein modernes React-Frontend.

---

## Architektur-Überblick

```plaintext
[Wago 750 / Siemens S7-1500]
        |
      (MQTT)
        |
[MQTT-Consumer] -- MongoDB
        |
    [REST-API]
        |
    [Frontend (React)]
```

### Komponenten-Beschreibung

- **MQTT-Consumer**: Abonniert SPS-Themen, speichert Werte in MongoDB
- **MongoDB**: Zentrale Persistenz für alle Messdaten und Status
- **REST-API**: Liefert die aktuellen Werte an das Frontend & nimmt Steuerbefehle entgegen (POST → MQTT)
- **Frontend**: Zeigt SPS-Status in Echtzeit und ermöglicht Steuerbefehle

---

## Projektstruktur

```
common/
  dto/          # Datenübertragungsobjekte (z.B. ControlCommand)
  model/        # Zentrale Datenmodelle für Siemens/Wago

mqttconsumer/
  config/       # MQTT-Verbindung & Topic-Konfiguration
  repository/   # MongoDB-Repositories für empfangene Werte
  service/      # Nachrichtenauswertung, Logging
  ...           # Application Bootstrap etc.

restapi/
  config/       # MQTT-Outbound-Config (für Steuerbefehle)
  controller/   # REST-Endpunkte für Siemens, Wago, Health
  repository/   # MongoDB-Repositories (REST-seitig)
  service/      # Business-Logik für REST-API
  ...           # Application Bootstrap etc.

frontend/
  src/
    App.js      # Haupt-UI (React)
    components/ # Karten, Buttons etc.
  ...

docker-compose.yml (oder einzelne Dockerfiles)
```

---

## Wichtigste Komponenten

### MQTT-Consumer (Spring Boot)
- Empfängt MQTT-Nachrichten von SPSen
- Persistiert Messdaten (Wago: Status/Lampen, Siemens: Temperaturen etc.)
- Monitoring/Logging per Micrometer

### REST-API (Spring Boot)
- Stellt aktuelle Daten über HTTP-Endpoints bereit (z.B. `/api/siemens/temperatur/ist/latest`)
- POST-Endpunkt `/api/wago/control` zur Steuerung (sendet MQTT-Befehl)

### Frontend (React)
- Live-Visualisierung aller Lampen/Temperaturen
- Ermöglicht Steuerung per Button (POST an REST-API)
- Pollt Backend in Intervallen

### MongoDB
- Zentrale Speicherung aller empfangenen und gesendeten Werte

---

## Wichtige Endpunkte

### Health
- `GET /api/health`

### Siemens (Temperaturen)
- `GET /api/siemens/temperatur/ist/latest`
- `GET /api/siemens/temperatur/soll/latest`
- `GET /api/siemens/temperatur/differenz/latest`

### Wago (Lampenstatus)
- `GET /api/wago/status/latest`
- `POST /api/wago/control` (Body: `{ "command": <0-3> }`)

---

## Tests

### Unit-Tests
- Für alle Kernkomponenten (Controller, Services, MQTT-Handler)
- Testen positives & negatives Verhalten, inkl. Fehlerszenarien

### Integrationstests
- **Testcontainers** für MongoDB & Mosquitto
- Prüfen die End-to-End-Funktionalität:
    - Speicherung und Abruf von Messdaten über REST und Datenbank
    - POST auf `/api/wago/control` erzeugt tatsächlich eine MQTT-Nachricht am richtigen Topic

---