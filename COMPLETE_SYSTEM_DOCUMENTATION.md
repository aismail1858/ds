# 📚 Distributed E-Commerce System - Vollständige Dokumentation

## 🎯 Inhaltsverzeichnis

1. [System-Übersicht](#1-system-übersicht)
2. [Architektur-Details](#2-architektur-details)
3. [Technische Komponenten](#3-technische-komponenten)
4. [Installation und Setup](#4-installation-und-setup)
5. [Konfiguration](#5-konfiguration)[COMPLETE_SYSTEM_DOCUMENTATION.md](COMPLETE_SYSTEM_DOCUMENTATION.md)
6. [Anwendung starten](#6-anwendung-starten)
7. [System-Monitoring](#7-system-monitoring)
8. [Troubleshooting](#8-troubleshooting)
9. [Erweiterte Features](#9-erweiterte-features)
10. [Entwickler-Handbuch](#10-entwickler-handbuch)
11. [Testing und Validation](#11-testing-und-validation)
12. [Performance-Optimierung](#12-performance-optimierung)

---

## 1. System-Übersicht

### 1.1 Was ist dieses System?

**Distributed E-Commerce Marketplace System** ist eine komplexe, verteilte Anwendung, die einen Online-Marktplatz simuliert. Das System demonstriert moderne Microservices-Architektur mit:

- **Mehrere Marketplace-Instanzen** (Koordinatoren)
- **Mehrere Seller-Services** (Verkäufer)
- **Distributed Transaction Processing** (Verteilte Transaktionsverarbeitung)
- **Saga Pattern Implementation** (Saga-Muster für verteilte Transaktionen)
- **Realistic Failure Simulation** (Realistische Fehlerbehandlung)

### 1.2 Hauptfunktionen

#### 🛒 **E-Commerce-Funktionalität:**
- **Bestellverarbeitung:** Automatische Verarbeitung von Kundenbestellungen
- **Inventory-Management:** Echtzeit-Bestandsverwaltung
- **Reservierungssystem:** Temporäre Produktreservierungen
- **Multi-Seller-Support:** Unterstützung mehrerer Verkäufer gleichzeitig

#### 🔄 **Distributed Systems Features:**
- **Saga Orchestration:** Koordination verteilter Transaktionen
- **Failure Handling:** Automatische Fehlerbehandlung und Rollback
- **Load Balancing:** Verteilung der Last auf mehrere Services
- **Fault Tolerance:** Ausfallsicherheit durch Redundanz

#### 🎭 **Simulation & Testing:**
- **Realistic Failure Patterns:** Simulation realer Systemausfälle
- **Performance Testing:** Belastungstests für Skalierbarkeit
- **Monitoring:** Umfassendes System-Monitoring
- **Logging:** Detaillierte Protokollierung aller Aktivitäten

---

## 2. Architektur-Details

### 2.1 High-Level-Architektur

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT REQUESTS                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
    ┌─────────────────┴───────────────────┐
    │                                     │
    ▼                                     ▼
┌─────────────────┐                 ┌─────────────────┐
│  MARKETPLACE1   │                 │  MARKETPLACE2   │
│  (Coordinator)  │                 │  (Coordinator)  │
└─────────┬───────┘                 └─────────┬───────┘
          │                                   │
          └─────────────────┬─────────────────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
            ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │   SELLER1   │ │   SELLER2   │ │   SELLER3   │
    │  (Service)  │ │  (Service)  │ │  (Service)  │
    └─────────────┘ └─────────────┘ └─────────────┘
            │               │               │
            ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │   SELLER4   │ │   SELLER5   │ │  INVENTORY  │
    │  (Service)  │ │  (Service)  │ │  DATABASE   │
    └─────────────┘ └─────────────┘ └─────────────┘
```

### 2.2 Component-Breakdown

#### 🏢 **Marketplace-Services (Coordinators):**
- **Rolle:** Orchestrieren Bestellungen zwischen Sellern
- **Anzahl:** 2 Instanzen für Hochverfügbarkeit
- **Hauptaufgaben:**
  - Bestellungen entgegennehmen
  - Saga-Transaktionen koordinieren
  - Failure-Handling und Rollback
  - Load-Balancing zwischen Sellern

#### 🏪 **Seller-Services (Participants):**
- **Rolle:** Verwalten Produktinventare und Bestellungen
- **Anzahl:** 5 Instanzen für Skalierbarkeit
- **Hauptaufgaben:**
  - Inventory-Management
  - Produktreservierungen
  - Bestellbestätigungen
  - Failure-Simulation

### 2.3 Kommunikationspattern

#### 🔄 **Request-Response Pattern:**
```
Marketplace → Seller: RESERVE_REQUEST
Seller → Marketplace: RESERVE_RESPONSE

Marketplace → Seller: CONFIRM_REQUEST
Seller → Marketplace: CONFIRM_RESPONSE

Marketplace → Seller: CANCEL_REQUEST
Seller → Marketplace: CANCEL_RESPONSE
```

#### 📡 **Message Format:**
```json
{
  "type": "RESERVE|CONFIRM|CANCEL",
  "orderId": "ORDER-001",
  "productId": "P1",
  "quantity": 5,
  "sellerId": "seller1",
  "reservationId": "seller1-R1",
  "success": true|false,
  "timestamp": "2025-07-16T20:30:00Z"
}
```

---

## 3. Technische Komponenten

### 3.1 Technologie-Stack

#### 🛠️ **Core Technologies:**
- **Java 11+:** Hauptprogrammiersprache
- **Maven:** Build-Management und Dependency-Management
- **ZeroMQ (JeroMQ):** Message-Passing-Bibliothek
- **Gson:** JSON-Serialisierung
- **Docker:** Containerisierung
- **Docker Compose:** Multi-Container-Orchestrierung

#### 📦 **Dependencies (pom.xml):**
```xml
<dependencies>
    <!-- ZeroMQ für Message-Passing -->
    <dependency>
        <groupId>org.zeromq</groupId>
        <artifactId>jeromq</artifactId>
        <version>0.5.3</version>
    </dependency>
    
    <!-- JSON-Verarbeitung -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
    
    <!-- Gemeinsame Komponenten -->
    <dependency>
        <groupId>com.distributed</groupId>
        <artifactId>common</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 3.2 Projekt-Struktur

```
distributed-systems/
├── common/                          # Gemeinsame Komponenten
│   ├── src/main/java/common/
│   │   ├── CircuitBreaker.java      # Circuit-Breaker-Pattern
│   │   ├── IdempotencyManager.java  # Idempotenz-Management
│   │   ├── Message.java             # Message-Datenstruktur
│   │   ├── OrderStatus.java         # Bestellstatus-Enum
│   │   ├── RetryManager.java        # Retry-Logik
│   │   └── SagaState.java          # Saga-State-Management
│   └── pom.xml
├── marketplace/                     # Marketplace-Service
│   ├── src/main/java/marketplace/
│   │   ├── AsyncMessageBroker.java  # Async-Message-Handling
│   │   ├── MarketplaceApp.java      # Hauptanwendung
│   │   ├── Order.java              # Bestellungs-Datenstruktur
│   │   ├── OrderProcessor.java      # Bestellverarbeitung
│   │   ├── SagaOrchestrator.java    # Saga-Koordination
│   │   └── SagaStateManager.java    # Saga-State-Persistence
│   ├── config.properties            # Standard-Konfiguration
│   ├── enhanced-config.properties   # Erweiterte Konfiguration
│   ├── orders.json                  # Beispiel-Bestellungen
│   ├── Dockerfile                   # Container-Definition
│   └── pom.xml
├── seller/                          # Seller-Service
│   ├── src/main/java/seller/
│   │   ├── AdvancedFailureSimulator.java  # Erweiterte Fehlerbehandlung
│   │   ├── EnhancedInventory.java         # Verbessertes Inventory
│   │   ├── FailureSimulator.java          # Basis-Fehlerbehandlung
│   │   ├── Inventory.java                 # Basis-Inventory
│   │   └── SellerApp.java                 # Hauptanwendung
│   ├── config.properties            # Standard-Konfiguration
│   ├── enhanced-config.properties   # Erweiterte Konfiguration
│   ├── Dockerfile                   # Container-Definition
│   └── pom.xml
├── lib/                             # Externe Bibliotheken
│   ├── common-1.0-SNAPSHOT.jar
│   ├── gson-2.10.1.jar
│   ├── jeromq-0.5.3.jar
│   └── jnacl-1.0.0.jar
├── logs/                            # Log-Dateien
│   ├── marketplace.log
│   ├── seller-seller1.log
│   ├── seller-seller2.log
│   └── ...
├── saga-states/                     # Saga-State-Persistence
│   └── *.json                       # Gespeicherte Saga-States
├── docker-compose.yml               # Multi-Container-Konfiguration
├── run.sh                          # Docker-Start-Script
├── stop.sh                         # Docker-Stop-Script
├── test-enhanced-system.sh         # Erweiterte Test-Suite
└── COMPLETE_SYSTEM_DOCUMENTATION.md # Diese Dokumentation
```

### 3.3 Klassen-Hierarchie

#### 🏢 **Marketplace-Komponenten:**

```java
// Haupt-Orchestrator
class MarketplaceApp {
    - Properties config
    - OrderProcessor orderProcessor
    + main(String[] args)
    + loadConfiguration()
    + startServices()
}

// Bestellverarbeitung
class OrderProcessor {
    - SagaOrchestrator sagaOrchestrator
    - AsyncMessageBroker messageBroker
    + processOrder(Order order)
    + handleOrderResult(OrderResult result)
}

// Saga-Koordination
class SagaOrchestrator {
    - Map<String, SagaInstance> activeSagas
    - SagaStateManager stateManager
    + processOrder(Order order)
    + executeReservationPhase(Order order)
    + executeConfirmationPhase(Order order)
    + executeCancellationPhase(Order order)
}
```

#### 🏪 **Seller-Komponenten:**

```java
// Haupt-Service
class SellerApp {
    - String sellerId
    - Inventory inventory
    - FailureSimulator failureSimulator
    + main(String[] args)
    + handleRequests()
    + processReservation(Message request)
    + processConfirmation(Message request)
    + processCancellation(Message request)
}

// Inventory-Management
class Inventory {
    - Map<String, Integer> stock
    - Map<String, Reservation> reservations
    + reserve(String productId, int quantity)
    + confirm(String reservationId)
    + cancel(String reservationId)
    + getAvailableStock(String productId)
}
```

---

## 4. Installation und Setup

### 4.1 Systemanforderungen

#### 🖥️ **Hardware-Anforderungen:**
- **RAM:** Mindestens 4GB, empfohlen 8GB
- **CPU:** 2+ Kerne, empfohlen 4+ Kerne
- **Festplatte:** 2GB freier Speicherplatz
- **Netzwerk:** Stabile Internetverbindung für Docker-Images

#### 💿 **Software-Anforderungen:**
- **Betriebssystem:** Linux (Ubuntu 18+), Windows 10+, macOS 10.14+
- **Java:** JDK 11 oder höher
- **Maven:** 3.6+ für Build-Management
- **Docker:** 20.10+ für Containerisierung
- **Docker Compose:** 1.29+ für Multi-Container-Orchestrierung
- **Git:** Für Versionskontrolle

### 4.2 Java-Installation

#### 🔧 **Ubuntu/Debian:**
```bash
# Java 11 installieren
sudo apt update
sudo apt install openjdk-11-jdk

# Installation verifizieren
java -version
javac -version

# JAVA_HOME setzen
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

#### 🔧 **Windows:**
```powershell
# Mit Chocolatey
choco install openjdk11

# Oder manuell von https://adoptopenjdk.net/
# JAVA_HOME in Systemvariablen setzen
```

#### 🔧 **macOS:**
```bash
# Mit Homebrew
brew install openjdk@11

# JAVA_HOME setzen
export JAVA_HOME=/usr/local/opt/openjdk@11
echo 'export JAVA_HOME=/usr/local/opt/openjdk@11' >> ~/.zshrc
source ~/.zshrc
```

### 4.3 Maven-Installation

#### 🔧 **Ubuntu/Debian:**
```bash
# Maven installieren
sudo apt install maven

# Installation verifizieren
mvn -version
```

#### 🔧 **Windows:**
```powershell
# Mit Chocolatey
choco install maven

# Oder manuell von https://maven.apache.org/
```

#### 🔧 **macOS:**
```bash
# Mit Homebrew
brew install maven

# Installation verifizieren
mvn -version
```

### 4.4 Docker-Installation

#### 🐳 **Ubuntu/Debian:**
```bash
# Docker installieren
sudo apt update
sudo apt install docker.io docker-compose

# Docker-Service starten
sudo systemctl start docker
sudo systemctl enable docker

# Benutzer zur docker-Gruppe hinzufügen
sudo usermod -aG docker $USER
newgrp docker

# Installation verifizieren
docker --version
docker-compose --version
```

#### 🐳 **Windows:**
```powershell
# Docker Desktop von https://www.docker.com/products/docker-desktop herunterladen
# Installieren und starten
```

#### 🐳 **macOS:**
```bash
# Docker Desktop von https://www.docker.com/products/docker-desktop herunterladen
# Oder mit Homebrew
brew install --cask docker
```

### 4.5 Projekt-Setup

#### 📥 **Projekt herunterladen:**
```bash
# Zu Projekt-Verzeichnis navigieren
cd /path/to/distributed-systems

# Projekt-Struktur verifizieren
ls -la
# Sollte zeigen: common/, marketplace/, seller/, docker-compose.yml, etc.
```

#### 🏗️ **Build-Prozess:**
```bash
# 1. Common-Modul bauen
cd common
mvn clean install
cd ..

# 2. Marketplace-Modul bauen
cd marketplace
mvn clean compile
mvn package -DskipTests
cd ..

# 3. Seller-Modul bauen
cd seller
mvn clean compile
mvn package -DskipTests
cd ..

# 4. Dependencies sammeln
cd marketplace
mvn dependency:copy-dependencies -DoutputDirectory=../lib
cd ../seller
mvn dependency:copy-dependencies -DoutputDirectory=../lib
cd ..

# 5. Build-Status verifizieren
ls -la lib/
ls -la marketplace/target/
ls -la seller/target/
```

---

## 5. Konfiguration

### 5.1 Marketplace-Konfiguration

#### 📄 **Standard-Konfiguration (marketplace/config.properties):**
```properties
# Marketplace-Grundkonfiguration
marketplace.id=marketplace1
marketplace.port=5555
order.delay.ms=5000
request.timeout.ms=5000

# Seller-Endpoints
seller1.endpoint=tcp://seller1:6001
seller2.endpoint=tcp://seller2:6002
seller3.endpoint=tcp://seller3:6003
seller4.endpoint=tcp://seller4:6004
seller5.endpoint=tcp://seller5:6005

# Basis-Fehlerbehandlung
failure.simulation.enabled=true
failure.probability=0.1
```

#### 📄 **Erweiterte Konfiguration (marketplace/enhanced-config.properties):**
```properties
# Erweiterte Marketplace-Konfiguration
marketplace.id=marketplace1
marketplace.router.port=5555
request.timeout.ms=10000

# Saga-Konfiguration
saga.timeout.seconds=120
saga.processing.threads=50
saga.state.directory=./saga-states

# Retry-Konfiguration
retry.max.attempts=5
retry.base.delay.ms=1000
retry.backoff.multiplier=2.0
retry.max.delay.ms=30000

# Circuit-Breaker-Konfiguration
circuit.breaker.failure.threshold=10
circuit.breaker.timeout.ms=60000
circuit.breaker.success.threshold=5

# Order-Processing-Konfiguration
order.processing.threads=20
order.delay.ms=2000

# Erweiterte Fehlerbehandlung
failure.no.response=0.03
failure.processing=0.08
failure.out.of.stock=0.12
failure.network.partition=0.01
failure.slow.response=0.15
failure.corruption=0.005

# Failure-Pattern-Konfiguration
pattern.cascading.multiplier=1.5
pattern.cascading.max.consecutive=3
pattern.periodic.interval.ms=3600000
pattern.periodic.duration.ms=180000
pattern.burst.probability=0.7
pattern.burst.duration.ms=45000
pattern.recovery.improvement.factor=0.85
pattern.recovery.success.threshold=8
```

### 5.2 Seller-Konfiguration

#### 📄 **Standard-Konfiguration (seller/config.properties):**
```properties
# Seller-Grundkonfiguration
seller.inventory.size=50
seller.processing.delay.ms=200
marketplace.endpoint=tcp://marketplace1:5555

# Basis-Fehlerbehandlung
failure.no_response.probability=0.05
failure.processing.probability=0.10
failure.out_of_stock.probability=0.15
failure.slow_response.probability=0.20
```

#### 📄 **Erweiterte Konfiguration (seller/enhanced-config.properties):**
```properties
# Erweiterte Seller-Konfiguration
seller.inventory.size=75
seller.processing.delay.ms=150
reservation.timeout.ms=300000
cleanup.interval.seconds=60

# Erweiterte Fehlerbehandlung
failure.no.response=0.04
failure.processing=0.10
failure.out.of.stock=0.15
failure.network.partition=0.02
failure.slow.response=0.20
failure.corruption=0.01

# Failure-Pattern-Konfiguration
pattern.cascading.multiplier=2.0
pattern.cascading.max.consecutive=5
pattern.periodic.interval.ms=3600000
pattern.periodic.duration.ms=300000
pattern.burst.probability=0.8
pattern.burst.duration.ms=30000
pattern.recovery.improvement.factor=0.9
pattern.recovery.success.threshold=10

# Idempotenz-Konfiguration
idempotency.retention.time.ms=1800000
```

### 5.3 Docker-Konfiguration

#### 📄 **Docker Compose (docker-compose.yml):**
```yaml
networks:
  marketplace-net:
    driver: bridge

services:
  # 5 Seller-Services
  seller1:
    build:
      context: ./seller
      args:
        - MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
    container_name: seller1
    environment:
      - SELLER_ID=seller1
      - SELLER_PORT=6001
    networks:
      - marketplace-net
    volumes:
      - maven-repo:/root/.m2

  seller2:
    build:
      context: ./seller
      args:
        - MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
    container_name: seller2
    environment:
      - SELLER_ID=seller2
      - SELLER_PORT=6002
    networks:
      - marketplace-net
    volumes:
      - maven-repo:/root/.m2

  seller3:
    build:
      context: ./seller
      args:
        - MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
    container_name: seller3
    environment:
      - SELLER_ID=seller3
      - SELLER_PORT=6003
    networks:
      - marketplace-net
    volumes:
      - maven-repo:/root/.m2

  seller4:
    build:
      context: ./seller
      args:
        - MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
    container_name: seller4
    environment:
      - SELLER_ID=seller4
      - SELLER_PORT=6004
    networks:
      - marketplace-net
    volumes:
      - maven-repo:/root/.m2

  seller5:
    build:
      context: ./seller
      args:
        - MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
    container_name: seller5
    environment:
      - SELLER_ID=seller5
      - SELLER_PORT=6005
    networks:
      - marketplace-net
    volumes:
      - maven-repo:/root/.m2

  # 2 Marketplace-Services
  marketplace1:
    build:
      context: ./marketplace
      args:
        - MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
    container_name: marketplace1
    environment:
      - MARKETPLACE_ID=MP1
    depends_on:
      - seller1
      - seller2
      - seller3
      - seller4
      - seller5
    networks:
      - marketplace-net
    volumes:
      - maven-repo:/root/.m2

  marketplace2:
    build:
      context: ./marketplace
      args:
        - MAVEN_OPTS=-Dmaven.repo.local=/root/.m2/repository
    container_name: marketplace2
    environment:
      - MARKETPLACE_ID=MP2
    depends_on:
      - seller1
      - seller2
      - seller3
      - seller4
      - seller5
    networks:
      - marketplace-net
    volumes:
      - maven-repo:/root/.m2

volumes:
  maven-repo:
```

### 5.4 Konfiguration anpassen

#### 🔧 **Marketplace-Konfiguration ändern:**
```bash
# Standard-Konfiguration bearbeiten
nano marketplace/config.properties

# Erweiterte Konfiguration bearbeiten
nano marketplace/enhanced-config.properties

# Wichtige Parameter:
# - marketplace.id: Eindeutige Marketplace-ID
# - request.timeout.ms: Timeout für Anfragen
# - order.delay.ms: Verzögerung zwischen Bestellungen
# - failure.probability: Ausfallwahrscheinlichkeit
```

#### 🔧 **Seller-Konfiguration ändern:**
```bash
# Standard-Konfiguration bearbeiten
nano seller/config.properties

# Erweiterte Konfiguration bearbeiten
nano seller/enhanced-config.properties

# Wichtige Parameter:
# - seller.inventory.size: Anfangsbestand pro Produkt
# - seller.processing.delay.ms: Verarbeitungszeit
# - failure.*.probability: Verschiedene Ausfallwahrscheinlichkeiten
```

---

## 6. Anwendung starten

### 6.1 Docker-basierte Ausführung (Empfohlen)

#### 🐳 **Standard-System starten:**
```bash
# Zum Projekt-Verzeichnis navigieren
cd /path/to/distributed-systems

# System starten
./run.sh

# Erwartete Ausgabe:
# =========================================
# Starting Distributed Marketplace System
# =========================================
# Cleaning up old containers...
# Starting services...
# ...
# ✓ System started!
```

#### 🐳 **System-Status prüfen:**
```bash
# Container-Status anzeigen
docker ps

# Erwartete Ausgabe:
# CONTAINER ID   IMAGE                              COMMAND               CREATED         STATUS         PORTS     NAMES
# abc123def456   distributed-systems-marketplace1   "java -jar app.jar"   30 seconds ago   Up 29 seconds             marketplace1
# def456ghi789   distributed-systems-marketplace2   "java -jar app.jar"   30 seconds ago   Up 29 seconds             marketplace2
# ghi789jkl012   distributed-systems-seller1        "java -jar app.jar"   30 seconds ago   Up 30 seconds             seller1
# ...

# Services-Status mit Docker Compose
docker-compose ps

# Logs anzeigen
docker-compose logs -f

# Spezifische Service-Logs
docker-compose logs -f marketplace1
docker-compose logs -f seller1
```

#### 🐳 **System stoppen:**
```bash
# System stoppen
./stop.sh

# Erwartete Ausgabe:
# =========================================
# Stopping Distributed Marketplace System
# =========================================
# ...
# ✓ System stopped!
```

### 6.2 Native Java-Ausführung (Erweiterte Features)

#### ☕ **Erweiterte System-Suite:**
```bash
# Erweiterte Test-Suite verfügbar machen
chmod +x test-enhanced-system.sh

# Hilfe anzeigen
./test-enhanced-system.sh

# Erwartete Ausgabe:
# Enhanced Distributed System Test Script
# Usage: ./test-enhanced-system.sh {start|stop|status|stress|recovery|features|clean}
```

#### ☕ **System starten:**
```bash
# Erweiterte System-Suite starten
./test-enhanced-system.sh start

# Erwartete Ausgabe:
# 🚀 Starting Enhanced System
# 🏬 Starting Enhanced Marketplace
# ✅ Marketplace started (PID: 1234)
# 🏪 Starting Enhanced Seller: seller1 on port 6001
# ✅ Seller seller1 started (PID: 1235)
# ...
# ✅ All services started
```

#### ☕ **System-Status:**
```bash
# Status prüfen
./test-enhanced-system.sh status

# Erwartete Ausgabe:
# 📊 System Status
# ================
# ✅ Marketplace: RUNNING
# ✅ Seller1: RUNNING
# ✅ Seller2: RUNNING
# ...
# 📈 Recent Activity:
# ===================
# Marketplace (last 5 lines):
# Order ORDER-001 completed successfully
# ...
```

### 6.3 Manuelle Ausführung

#### 🔧 **Einzelne Komponenten starten:**
```bash
# 1. Marketplace starten
cd marketplace
java -Dconfig.file=config.properties \
     -jar target/marketplace.jar &
MARKETPLACE_PID=$!

# 2. Seller starten (für jeden Seller wiederholen)
cd ../seller
SELLER_ID=seller1 MARKETPLACE_ENDPOINT=tcp://localhost:5555 \
java -Dconfig.file=config.properties \
     -jar target/seller.jar &
SELLER1_PID=$!

# 3. Weitere Seller starten
SELLER_ID=seller2 MARKETPLACE_ENDPOINT=tcp://localhost:5555 \
java -Dconfig.file=config.properties \
     -jar target/seller.jar &
SELLER2_PID=$!

# ... (wiederholen für seller3, seller4, seller5)
```

#### 🔧 **Prozesse beenden:**
```bash
# Alle Prozesse beenden
kill $MARKETPLACE_PID $SELLER1_PID $SELLER2_PID $SELLER3_PID $SELLER4_PID $SELLER5_PID

# Oder alle Java-Prozesse beenden
pkill -f "java.*marketplace"
pkill -f "java.*seller"
```

---

## 7. System-Monitoring

### 7.1 Real-time Monitoring

#### 📊 **Docker-Container-Monitoring:**
```bash
# Container-Ressourcenverbrauch
docker stats

# Erwartete Ausgabe:
# CONTAINER ID   NAME          CPU %     MEM USAGE / LIMIT     MEM %     NET I/O           BLOCK I/O         PIDS
# abc123def456   marketplace1  0.50%     120.5MiB / 1.944GiB   6.05%     1.23kB / 890B     0B / 0B           25
# def456ghi789   seller1       0.25%     95.2MiB / 1.944GiB    4.78%     890B / 1.23kB     0B / 0B           20
# ...

# Container-Logs in Echtzeit
docker-compose logs -f --tail=50

# Spezifische Container-Logs
docker logs -f marketplace1
docker logs -f seller1
```

#### 📊 **Native Java-Monitoring:**
```bash
# Prozess-Monitoring
./test-enhanced-system.sh status

# Erwartete Ausgabe:
# 📊 System Status
# ================
# ✅ Marketplace: RUNNING
# ✅ Seller1: RUNNING
# ...
# 📈 Recent Activity:
# Marketplace (last 5 lines):
# Order ORDER-001 completed successfully
# ...
# 🔍 Active Sagas: 2

# Kontinuierliches Monitoring
watch -n 5 './test-enhanced-system.sh status'
```

### 7.2 Log-Analyse

#### 📄 **Log-Dateien-Struktur:**
```
logs/
├── marketplace.log          # Marketplace-Hauptlogs
├── seller-seller1.log       # Seller1-Logs
├── seller-seller2.log       # Seller2-Logs
├── seller-seller3.log       # Seller3-Logs
├── seller-seller4.log       # Seller4-Logs
└── seller-seller5.log       # Seller5-Logs
```

#### 📄 **Log-Analyse-Befehle:**
```bash
# Marketplace-Logs anzeigen
tail -f logs/marketplace.log

# Seller-Logs anzeigen
tail -f logs/seller-seller1.log

# Alle Logs gleichzeitig
tail -f logs/*.log

# Fehler-Logs filtern
grep -i error logs/*.log
grep -i exception logs/*.log
grep -i failed logs/*.log

# Erfolgreiche Bestellungen filtern
grep -i "completed successfully" logs/marketplace.log

# Reservierungen verfolgen
grep -i "reserved" logs/seller-*.log
```

#### 📄 **Log-Format verstehen:**
```
# Marketplace-Log-Format:
2025-07-16 20:30:15 [INFO] === Processing Order: ORDER-001 ===
2025-07-16 20:30:15 [INFO] Starting SAGA for order: ORDER-001
2025-07-16 20:30:15 [INFO]   Reserving 5x P1 at seller1
2025-07-16 20:30:15 [INFO]     ✓ Reserved with ID: seller1-R1
2025-07-16 20:30:15 [INFO] ✓ Order ORDER-001 completed successfully!

# Seller-Log-Format:
2025-07-16 20:30:15 [INFO] Received request: {"type":"RESERVE","orderId":"ORDER-001","productId":"P1","quantity":5}
2025-07-16 20:30:15 [INFO] Reserved: 5x P1 (ID: seller1-R1)
2025-07-16 20:30:15 [INFO] Received request: {"type":"CONFIRM","reservationId":"seller1-R1"}
2025-07-16 20:30:15 [INFO] Confirmed reservation: seller1-R1
```

### 7.3 Performance-Metriken

#### 📈 **Marketplace-Metriken:**
```bash
# Bestellstatistiken extrahieren
grep "Processing Summary" logs/marketplace.log

# Erwartete Ausgabe:
# === Processing Summary ===
# Total orders: 5
# Successful: 2
# Failed: 3
# Success rate: 40.0%

# Durchschnittliche Bearbeitungszeit
grep "completed successfully" logs/marketplace.log | wc -l

# Fehlerrate berechnen
grep -c "failed" logs/marketplace.log
```

#### 📈 **Seller-Metriken:**
```bash
# Reservierungsstatistiken
grep -c "Reserved:" logs/seller-*.log

# Bestätigungsstatistiken
grep -c "Confirmed:" logs/seller-*.log

# Stornierungsstatistiken
grep -c "Cancelled:" logs/seller-*.log

# Fehlerstatistiken
grep -c "failed" logs/seller-*.log
```

### 7.4 System-Health-Checks

#### 🔍 **Automatische Health-Checks:**
```bash
# Script für Health-Check erstellen
cat > health-check.sh << 'EOF'
#!/bin/bash

echo "=== System Health Check ==="
echo "Timestamp: $(date)"
echo ""

# Container-Status prüfen
echo "Container Status:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.RunningFor}}"
echo ""

# Ressourcenverbrauch prüfen
echo "Resource Usage:"
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"
echo ""

# Log-Fehler prüfen
echo "Recent Errors:"
grep -i error logs/*.log | tail -5
echo ""

# Erfolgreiche Bestellungen prüfen
echo "Recent Successes:"
grep -i "completed successfully" logs/marketplace.log | tail -3
echo ""

echo "=== Health Check Complete ==="
EOF

chmod +x health-check.sh

# Health-Check ausführen
./health-check.sh
```

---

## 8. Troubleshooting

### 8.1 Häufige Probleme und Lösungen

#### ❌ **Problem: Container starten nicht**
```bash
# Diagnose
docker-compose ps
docker-compose logs

# Häufige Ursachen:
# 1. Port bereits belegt
sudo netstat -tulpn | grep :5555
sudo netstat -tulpn | grep :6001

# 2. Unzureichende Ressourcen
docker system df
docker system prune -f

# 3. Build-Fehler
docker-compose build --no-cache

# Lösung:
# 1. Alle Container stoppen
docker-compose down

# 2. System bereinigen
docker system prune -f
docker volume prune -f

# 3. Neu bauen und starten
docker-compose build --no-cache
docker-compose up -d
```

#### ❌ **Problem: Java-Prozesse starten nicht**
```bash
# Diagnose
./test-enhanced-system.sh status

# Häufige Ursachen:
# 1. JAVA_HOME nicht gesetzt
echo $JAVA_HOME
java -version

# 2. Fehlende JAR-Dateien
ls -la marketplace/target/*.jar
ls -la seller/target/*.jar

# 3. Fehlende Dependencies
ls -la lib/

# Lösung:
# 1. JAVA_HOME setzen
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# 2. Projekt neu bauen
cd common && mvn clean install && cd ..
cd marketplace && mvn clean package -DskipTests && cd ..
cd seller && mvn clean package -DskipTests && cd ..

# 3. Dependencies kopieren
cd marketplace && mvn dependency:copy-dependencies -DoutputDirectory=../lib && cd ..
cd seller && mvn dependency:copy-dependencies -DoutputDirectory=../lib && cd ..
```

#### ❌ **Problem: Kommunikation zwischen Services funktioniert nicht**
```bash
# Diagnose
docker network ls
docker network inspect distributed-systems_marketplace-net

# Häufige Ursachen:
# 1. Netzwerk-Isolation
# 2. Falsche Endpunkte
# 3. Firewall-Blockierung

# Lösung:
# 1. Netzwerk-Konnektivität testen
docker exec -it seller1 ping marketplace1
docker exec -it marketplace1 ping seller1

# 2. Endpunkte in Konfiguration prüfen
grep -i endpoint marketplace/config.properties
grep -i endpoint seller/config.properties

# 3. Firewall-Regeln prüfen
sudo ufw status
sudo iptables -L
```

#### ❌ **Problem: Hohe Fehlerrate**
```bash
# Diagnose
grep "Success rate" logs/marketplace.log

# Häufige Ursachen:
# 1. Zu hohe Fehlerbehandlung-Wahrscheinlichkeit
# 2. Unzureichende Ressourcen
# 3. Timing-Probleme

# Lösung:
# 1. Fehlerbehandlung-Konfiguration anpassen
nano marketplace/config.properties
# failure.probability=0.05  # Reduzieren von 0.1 auf 0.05

nano seller/config.properties
# failure.no_response.probability=0.02  # Reduzieren
# failure.processing.probability=0.05   # Reduzieren

# 2. Timeouts erhöhen
# request.timeout.ms=10000  # Erhöhen von 5000 auf 10000
# order.delay.ms=3000       # Erhöhen von 2000 auf 3000

# 3. System neu starten
./stop.sh
./run.sh
```

### 8.2 Debug-Modus

#### 🔍 **Erweiterte Debug-Ausgabe:**
```bash
# Debug-Modus für Docker aktivieren
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# Verbose-Logging aktivieren
docker-compose up -d --verbose

# Java-Debug-Modus aktivieren
# In marketplace/Dockerfile und seller/Dockerfile:
# CMD ["java", "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "app.jar"]

# Container mit Debug-Port starten
docker run -p 5005:5005 distributed-systems-marketplace1
```

#### 🔍 **Log-Level erhöhen:**
```bash
# Log4j-Konfiguration hinzufügen (log4j.properties)
cat > marketplace/src/main/resources/log4j.properties << 'EOF'
log4j.rootLogger=DEBUG, stdout, file

log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} [%p] %c{1}: %m%n

log4j.appender.file=org.apache.log4j.FileAppender
log4j.appender.file.File=../logs/marketplace-debug.log
log4j.appender.file.layout=org.apache.log4j.PatternLayout
log4j.appender.file.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} [%p] %c{1}: %m%n
EOF

# Ähnliche Konfiguration für seller/src/main/resources/log4j.properties
```

### 8.3 Performance-Troubleshooting

#### 📊 **Ressourcenverbrauch analysieren:**
```bash
# CPU-Verbrauch überwachen
top -p $(pgrep -f "java.*marketplace" | tr '\n' ',' | sed 's/,$//')

# Memory-Verbrauch überwachen
ps aux | grep -E "(marketplace|seller)" | grep -v grep

# Disk-I/O überwachen
iotop -p $(pgrep -f "java.*marketplace")

# Netzwerk-Traffic überwachen
netstat -i
ifconfig -a
```

#### 📊 **JVM-Tuning:**
```bash
# JVM-Parameter optimieren
# In test-enhanced-system.sh:
java -Xms512m -Xmx1024m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+PrintGCDetails \
     -XX:+PrintGCTimeStamps \
     -Dconfig.file=enhanced-config.properties \
     -jar target/marketplace.jar
```

---

## 9. Erweiterte Features

### 9.1 Enhanced System-Features

#### 🚀 **Erweiterte Funktionalitäten:**
Das System bietet zwei Modi:

**Standard-Modus (Docker):**
- Basis-REQ-REP-Pattern
- Einfache Fehlerbehandlung
- Grundlegende Inventory-Verwaltung

**Enhanced-Modus (Native Java):**
- ROUTER-DEALER-Pattern
- Erweiterte Fehlerbehandlung
- Saga-State-Persistence
- Circuit-Breaker-Pattern
- Retry-Mechanismus
- Idempotenz-Management

#### 🚀 **Enhanced-Modus aktivieren:**
```bash
# Enhanced System starten
./test-enhanced-system.sh start

# Erweiterte Features anzeigen
./test-enhanced-system.sh features

# Erwartete Ausgabe:
# 🚀 Enhanced Features Demonstration
# ==================================
# ✅ Implemented Features:
# • Fixed ZMQ ROUTER-DEALER messaging pattern
# • Added idempotency management for exactly-once processing
# • Implemented retry logic with exponential backoff
# • Added saga state persistence and recovery
# • Enhanced inventory with proper concurrency control
# • Implemented circuit breaker pattern
# • Added advanced failure simulation with realistic patterns
# • Improved error handling and compensation logic
```

### 9.2 Advanced Testing

#### 🔥 **Stress-Testing:**
```bash
# Stress-Test ausführen
./test-enhanced-system.sh stress

# Erwartete Ausgabe:
# 🔥 Running Stress Test with Enhanced Features
# ==============================================
# ✅ All services started
# 🔍 Monitoring system for 60 seconds...
# --- Check 1/12 ---
# 📊 System Status
# ...
# ✅ Stress test completed
```

#### 🔧 **Failure-Recovery-Test:**
```bash
# Failure-Recovery-Test ausführen
./test-enhanced-system.sh recovery

# Erwartete Ausgabe:
# 🔧 Demonstrating Failure Recovery
# ==================================
# ✅ System started with 3 sellers
# 💥 Simulating seller1 failure...
# 🔄 System running with 2 sellers for 30 seconds...
# 🔄 Restarting seller1...
# 🔍 Monitoring recovery for 30 seconds...
# ✅ Failure recovery demonstration completed
```

### 9.3 Konfigurierbare Failure-Patterns

#### 🎭 **Failure-Pattern-Typen:**

**Cascading Failures:**
```properties
# Kaskadierungseffekt - ein Fehler führt zu weiteren Fehlern
pattern.cascading.multiplier=2.0
pattern.cascading.max.consecutive=5
```

**Periodic Failures:**
```properties
# Periodische Ausfälle - simuliert Wartungsfenster
pattern.periodic.interval.ms=3600000      # Alle 1 Stunde
pattern.periodic.duration.ms=300000       # 5 Minuten lang
```

**Burst Failures:**
```properties
# Burst-Ausfälle - plötzliche Spitzen in der Fehlerrate
pattern.burst.probability=0.8
pattern.burst.duration.ms=30000
```

**Recovery Patterns:**
```properties
# Erholungsmuster - graduelle Verbesserung nach Ausfällen
pattern.recovery.improvement.factor=0.9
pattern.recovery.success.threshold=10
```

### 9.4 Saga-State-Management

#### 💾 **Saga-State-Persistence:**
```bash
# Saga-States anzeigen
ls -la saga-states/

# Erwartete Ausgabe:
# -rw-r--r-- 1 user user 1234 Jul 16 20:30 saga-ORDER-001.json
# -rw-r--r-- 1 user user 1234 Jul 16 20:30 saga-ORDER-002.json

# Saga-State-Inhalt anzeigen
cat saga-states/saga-ORDER-001.json

# Erwartete Ausgabe:
# {
#   "sagaId": "ORDER-001",
#   "orderId": "ORDER-001",
#   "status": "COMPLETED",
#   "reservations": [
#     {
#       "sellerId": "seller1",
#       "productId": "P1",
#       "quantity": 5,
#       "reservationId": "seller1-R1",
#       "status": "CONFIRMED"
#     }
#   ],
#   "timestamp": "2025-07-16T20:30:00Z"
# }
```

#### 💾 **Saga-Recovery:**
```bash
# System nach Crash neu starten
./test-enhanced-system.sh stop
./test-enhanced-system.sh start

# Logs auf Recovery-Meldungen prüfen
grep -i "recovery" logs/marketplace.log
grep -i "restored" logs/marketplace.log

# Erwartete Ausgabe:
# Saga state recovery initiated
# Restored 3 saga states from disk
# Recovery completed successfully
```

### 9.5 Circuit-Breaker-Pattern

#### ⚡ **Circuit-Breaker-Monitoring:**
```bash
# Circuit-Breaker-Status in Logs verfolgen
grep -i "circuit breaker" logs/marketplace.log

# Erwartete Ausgabe:
# Circuit breaker OPEN after 5 failures
# Circuit breaker attempting reset
# Circuit breaker CLOSED after successful operation
```

#### ⚡ **Circuit-Breaker-Konfiguration:**
```properties
# Circuit-Breaker-Einstellungen
circuit.breaker.failure.threshold=10    # Anzahl Fehler bis OPEN
circuit.breaker.timeout.ms=60000        # Timeout im OPEN-Status
circuit.breaker.success.threshold=5     # Erfolge bis CLOSED
```

---

## 10. Entwickler-Handbuch

### 10.1 Code-Struktur verstehen

#### 🏗️ **Architektur-Pattern:**

**Saga-Pattern-Implementierung:**
```java
// SagaOrchestrator.java
public class SagaOrchestrator {
    // Saga-Instanz verwalten
    private final Map<String, SagaInstance> activeSagas = new ConcurrentHashMap<>();
    
    public Order processOrder(Order order) throws Exception {
        String sagaId = UUID.randomUUID().toString();
        SagaInstance saga = new SagaInstance(sagaId, order);
        
        // Saga-State speichern
        stateManager.saveSagaState(sagaId, createSnapshot(saga));
        
        try {
            // Reservation-Phase
            executeReservationPhase(saga);
            
            // Confirmation-Phase
            executeConfirmationPhase(saga);
            
            // Erfolg - State bereinigen
            stateManager.removeSagaState(sagaId);
            return saga.getOrder();
            
        } catch (Exception e) {
            // Fehler - Compensation ausführen
            compensateSaga(saga);
            throw e;
        }
    }
}
```

**Circuit-Breaker-Pattern:**
```java
// CircuitBreaker.java
public class CircuitBreaker {
    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile long lastFailureTime = 0;
    
    public enum State { CLOSED, OPEN, HALF_OPEN }
    
    public <T> CompletableFuture<T> execute(
        Supplier<CompletableFuture<T>> operation, 
        String operationName) {
        
        State currentState = state;
        
        switch (currentState) {
            case CLOSED:
                return executeOperation(operation, operationName);
            case OPEN:
                if (shouldAttemptReset()) {
                    return attemptReset(operation, operationName);
                } else {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Circuit breaker is OPEN"));
                }
            case HALF_OPEN:
                return executeHalfOpenOperation(operation, operationName);
        }
    }
}
```

### 10.2 Neue Features entwickeln

#### 💡 **Neue Seller-Funktionalität hinzufügen:**

**1. Interface erweitern:**
```java
// seller/src/main/java/seller/SellerService.java
public interface SellerService {
    ReservationResult reserve(String productId, int quantity);
    ConfirmationResult confirm(String reservationId);
    CancellationResult cancel(String reservationId);
    
    // Neue Funktionalität
    ProductInfo getProductInfo(String productId);
    InventoryReport getInventoryReport();
}
```

**2. Implementation erstellen:**
```java
// seller/src/main/java/seller/SellerServiceImpl.java
@Override
public ProductInfo getProductInfo(String productId) {
    return new ProductInfo(
        productId,
        inventory.getAvailableStock(productId),
        inventory.getReservedStock(productId),
        inventory.getPrice(productId)
    );
}
```

**3. Message-Handler erweitern:**
```java
// seller/src/main/java/seller/SellerApp.java
private void handleRequest(Message request) {
    switch (request.getType()) {
        case "RESERVE":
            handleReservation(request);
            break;
        case "CONFIRM":
            handleConfirmation(request);
            break;
        case "CANCEL":
            handleCancellation(request);
            break;
        case "PRODUCT_INFO":  // Neue Funktionalität
            handleProductInfo(request);
            break;
        default:
            handleUnknownRequest(request);
    }
}
```

#### 💡 **Neue Marketplace-Funktionalität hinzufügen:**

**1. Order-Processing erweitern:**
```java
// marketplace/src/main/java/marketplace/OrderProcessor.java
public class OrderProcessor {
    
    // Neue Funktionalität: Batch-Processing
    public List<Order> processBatchOrders(List<Order> orders) {
        return orders.parallelStream()
            .map(this::processOrder)
            .collect(Collectors.toList());
    }
    
    // Neue Funktionalität: Priority-Orders
    public Order processPriorityOrder(Order order) {
        // Höhere Priorität in der Saga-Verarbeitung
        return sagaOrchestrator.processOrderWithPriority(order, Priority.HIGH);
    }
}
```

### 10.3 Testing-Framework erweitern

#### 🧪 **Unit-Tests hinzufügen:**

**1. Test-Struktur erstellen:**
```bash
# Test-Verzeichnisse erstellen
mkdir -p marketplace/src/test/java/marketplace
mkdir -p seller/src/test/java/seller
mkdir -p common/src/test/java/common

# JUnit-Dependencies hinzufügen (pom.xml)
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.8.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>4.6.1</version>
    <scope>test</scope>
</dependency>
```

**2. Saga-Orchestrator-Tests:**
```java
// marketplace/src/test/java/marketplace/SagaOrchestratorTest.java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class SagaOrchestratorTest {
    
    @Mock
    private AsyncMessageBroker messageBroker;
    
    @Mock
    private SagaStateManager stateManager;
    
    private SagaOrchestrator orchestrator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orchestrator = new SagaOrchestrator("test-marketplace", messageBroker, new Properties());
    }
    
    @Test
    void testSuccessfulOrderProcessing() {
        // Arrange
        Order order = new Order("ORDER-001", "customer1", "test-marketplace");
        order.addItem("P1", 5, "seller1");
        
        when(messageBroker.sendAsyncRequest(anyString(), any(Message.class)))
            .thenReturn(CompletableFuture.completedFuture(
                new Message("RESERVE_RESPONSE", "ORDER-001", true, "seller1-R1")));
        
        // Act
        Order result = orchestrator.processOrder(order);
        
        // Assert
        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        verify(messageBroker, times(2)).sendAsyncRequest(anyString(), any(Message.class));
    }
    
    @Test
    void testFailedOrderProcessing() {
        // Arrange
        Order order = new Order("ORDER-002", "customer2", "test-marketplace");
        order.addItem("P1", 100, "seller1");
        
        when(messageBroker.sendAsyncRequest(anyString(), any(Message.class)))
            .thenReturn(CompletableFuture.completedFuture(
                new Message("RESERVE_RESPONSE", "ORDER-002", false, "Insufficient stock")));
        
        // Act & Assert
        assertThrows(Exception.class, () -> orchestrator.processOrder(order));
        verify(messageBroker, times(1)).sendAsyncRequest(anyString(), any(Message.class));
    }
}
```

**3. Inventory-Tests:**
```java
// seller/src/test/java/seller/InventoryTest.java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class InventoryTest {
    
    private Inventory inventory;
    
    @BeforeEach
    void setUp() {
        inventory = new Inventory();
        inventory.addProduct("P1", 10);
        inventory.addProduct("P2", 20);
    }
    
    @Test
    void testSuccessfulReservation() {
        // Act
        String reservationId = inventory.reserve("P1", 5);
        
        // Assert
        assertNotNull(reservationId);
        assertEquals(5, inventory.getAvailableStock("P1"));
    }
    
    @Test
    void testInsufficientStock() {
        // Act & Assert
        assertNull(inventory.reserve("P1", 15));
        assertEquals(10, inventory.getAvailableStock("P1"));
    }
    
    @Test
    void testReservationConfirmation() {
        // Arrange
        String reservationId = inventory.reserve("P1", 5);
        
        // Act
        boolean confirmed = inventory.confirm(reservationId);
        
        // Assert
        assertTrue(confirmed);
        assertEquals(5, inventory.getAvailableStock("P1"));
    }
}
```

### 10.4 Deployment-Strategien

#### 🚀 **Docker-Image-Optimierung:**

**1. Multi-Stage-Build:**
```dockerfile
# marketplace/Dockerfile
FROM maven:3.8.4-openjdk-11-slim AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder /app/target/marketplace.jar app.jar
EXPOSE 5555
CMD ["java", "-jar", "app.jar"]
```

**2. Docker-Compose-Skalierung:**
```yaml
# docker-compose.yml
version: '3.8'
services:
  marketplace:
    build: ./marketplace
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          cpus: '0.25'
          memory: 256M
    environment:
      - MARKETPLACE_ID=${MARKETPLACE_ID:-marketplace}
      - JAVA_OPTS=-Xmx512m -Xms256m
```

**3. Kubernetes-Deployment:**
```yaml
# k8s/marketplace-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: marketplace-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: marketplace
  template:
    metadata:
      labels:
        app: marketplace
    spec:
      containers:
      - name: marketplace
        image: distributed-systems-marketplace:latest
        ports:
        - containerPort: 5555
        env:
        - name: MARKETPLACE_ID
          value: "marketplace"
        resources:
          limits:
            cpu: 500m
            memory: 512Mi
          requests:
            cpu: 250m
            memory: 256Mi
```

---

## 11. Testing und Validation

### 11.1 Automatisierte Test-Suites

#### 🧪 **Test-Kategorien:**

**1. Unit-Tests:**
```bash
# Unit-Tests ausführen
cd common && mvn test
cd ../marketplace && mvn test  
cd ../seller && mvn test

# Test-Coverage-Report generieren
mvn jacoco:report
```

**2. Integration-Tests:**
```bash
# Integration-Tests mit Docker
docker-compose -f docker-compose.test.yml up --abort-on-container-exit

# Integration-Tests mit Native Java
./test-enhanced-system.sh start
sleep 10
./integration-test.sh
./test-enhanced-system.sh stop
```

**3. Performance-Tests:**
```bash
# Stress-Test mit verschiedenen Lasten
./test-enhanced-system.sh stress

# Load-Test mit JMeter
jmeter -n -t performance-test.jmx -l results.jtl
```

### 11.2 Validation-Checkliste

#### ✅ **Funktionale Validierung:**

**Bestellverarbeitung:**
- [ ] Erfolgreiche Bestellung mit verfügbaren Produkten
- [ ] Bestellung mit unzureichendem Bestand
- [ ] Bestellung mit mehreren Produkten von verschiedenen Sellern
- [ ] Bestellung mit Seller-Ausfällen
- [ ] Bestellung mit Netzwerkfehlern

**Saga-Orchestrierung:**
- [ ] Erfolgreiche Saga-Abwicklung
- [ ] Saga-Rollback bei Fehlern
- [ ] Saga-State-Persistence
- [ ] Saga-Recovery nach Crash

**Inventory-Management:**
- [ ] Korrekte Reservierung
- [ ] Korrekte Bestätigung
- [ ] Korrekte Stornierung
- [ ] Concurrent-Access-Handling

#### ✅ **Performance-Validierung:**

**Durchsatz:**
- [ ] 100 Bestellungen/Minute
- [ ] 1000 Bestellungen/Minute
- [ ] 10000 Bestellungen/Minute

**Latenz:**
- [ ] < 1 Sekunde für einfache Bestellungen
- [ ] < 5 Sekunden für komplexe Bestellungen
- [ ] < 10 Sekunden für Batch-Verarbeitung

**Skalierbarkeit:**
- [ ] Horizontale Skalierung (mehr Seller)
- [ ] Vertikale Skalierung (mehr Ressourcen)
- [ ] Load-Balancing zwischen Marketplaces

### 11.3 Monitoring und Alerting

#### 📊 **Metriken-Collection:**

**1. Prometheus-Integration:**
```java
// marketplace/src/main/java/marketplace/MetricsCollector.java
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;

public class MetricsCollector {
    private static final Counter ordersProcessed = Counter.build()
        .name("orders_processed_total")
        .help("Total orders processed")
        .register();
    
    private static final Histogram orderProcessingTime = Histogram.build()
        .name("order_processing_duration_seconds")
        .help("Order processing time")
        .register();
    
    public static void startMetricsServer() throws IOException {
        HTTPServer server = new HTTPServer(8080);
    }
}
```

**2. Grafana-Dashboard:**
```json
{
  "dashboard": {
    "title": "Distributed E-Commerce System",
    "panels": [
      {
        "title": "Orders per Second",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(orders_processed_total[5m])",
            "legendFormat": "Orders/sec"
          }
        ]
      },
      {
        "title": "Success Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(orders_successful_total[5m]) / rate(orders_processed_total[5m]) * 100",
            "legendFormat": "Success %"
          }
        ]
      }
    ]
  }
}
```

---

## 12. Performance-Optimierung

### 12.1 JVM-Tuning

#### ⚡ **Memory-Optimierung:**

**1. Heap-Größe anpassen:**
```bash
# Für Marketplace (mehr Memory für Saga-States)
java -Xms1024m -Xmx2048m -jar marketplace.jar

# Für Seller (weniger Memory ausreichend)
java -Xms512m -Xmx1024m -jar seller.jar
```

**2. Garbage-Collection-Optimierung:**
```bash
# G1GC für niedrige Latenz
java -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar marketplace.jar

# ZGC für sehr niedrige Latenz (Java 11+)
java -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -jar marketplace.jar
```

**3. JVM-Monitoring:**
```bash
# GC-Logs aktivieren
java -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:gc.log -jar marketplace.jar

# JVM-Metriken sammeln
java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9999 -jar marketplace.jar
```

### 12.2 Netzwerk-Optimierung

#### 🌐 **ZeroMQ-Tuning:**

**1. Socket-Optionen:**
```java
// AsyncMessageBroker.java
socket.setHWM(1000);              // High Water Mark
socket.setLinger(0);              // Linger Time
socket.setRcvtimeo(5000);         // Receive Timeout
socket.setSndtimeo(5000);         // Send Timeout
socket.setTcpKeepalive(1);        // TCP Keepalive
```

**2. Connection-Pooling:**
```java
// ConnectionPool.java
public class ConnectionPool {
    private final BlockingQueue<ZMQ.Socket> availableConnections;
    private final int maxConnections;
    
    public ZMQ.Socket borrowConnection() throws InterruptedException {
        return availableConnections.poll(5, TimeUnit.SECONDS);
    }
    
    public void returnConnection(ZMQ.Socket socket) {
        availableConnections.offer(socket);
    }
}
```

### 12.3 Concurrency-Optimierung

#### 🔄 **Thread-Pool-Tuning:**

**1. Executor-Konfiguration:**
```java
// OrderProcessor.java
private final ExecutorService orderExecutor = new ThreadPoolExecutor(
    10,                           // Core Pool Size
    50,                           // Maximum Pool Size
    60L, TimeUnit.SECONDS,        // Keep Alive Time
    new LinkedBlockingQueue<>(1000),  // Work Queue
    new ThreadPoolExecutor.CallerRunsPolicy()  // Rejection Policy
);
```

**2. Async-Processing-Optimierung:**
```java
// CompletableFuture-Tuning
CompletableFuture.supplyAsync(() -> {
    // Heavy processing
    return processOrder(order);
}, customExecutor)
.thenApplyAsync(result -> {
    // Post-processing
    return finalizeOrder(result);
}, customExecutor)
.exceptionally(throwable -> {
    // Error handling
    return handleError(throwable);
});
```

### 12.4 Database-Optimierung

#### 💾 **Saga-State-Persistence-Optimierung:**

**1. Batch-Writing:**
```java
// SagaStateManager.java
private final BlockingQueue<SagaState> pendingWrites = new LinkedBlockingQueue<>();

private void batchWriteStates() {
    List<SagaState> batch = new ArrayList<>();
    pendingWrites.drainTo(batch, 100);  // Batch-Size: 100
    
    // Batch-Write zu Disk
    writeBatchToFile(batch);
}
```

**2. Asynchrone Persistence:**
```java
// Async-Persistence
CompletableFuture.runAsync(() -> {
    persistSagaState(sagaId, snapshot);
}, persistenceExecutor);
```

---

## 🎯 Fazit

Diese Dokumentation bietet einen umfassenden Überblick über das Distributed E-Commerce System. Das System demonstriert moderne Microservices-Architektur mit:

- **Distributed Transaction Processing** via Saga-Pattern
- **Fault Tolerance** durch Circuit-Breaker und Retry-Mechanismen
- **Scalability** durch horizontale Skalierung
- **Monitoring** und umfassende Logging-Funktionen
- **Container-Orchestrierung** mit Docker

Das System ist sowohl für **Lernzwecke** als auch für **Produktionsumgebungen** geeignet und bietet eine solide Grundlage für die Entwicklung verteilter Systeme.

---

## 📚 Weiterführende Ressourcen

- [Saga Pattern Documentation](https://microservices.io/patterns/data/saga.html)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [ZeroMQ Guide](https://zguide.zeromq.org/)
- [Docker Documentation](https://docs.docker.com/)
- [Microservices Patterns](https://microservices.io/patterns/)

**System-Version:** 1.0.0  
**Dokumentations-Version:** 1.0.0  
**Letzte Aktualisierung:** 16. Juli 2025