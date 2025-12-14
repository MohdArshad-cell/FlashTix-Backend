# ⚡ FlashTix - High Concurrency Ticketing Engine

FlashTix is a robust backend system designed to handle massive traffic surges (e.g., flash sales) while guaranteeing strict data consistency. It prevents race conditions and overbooking using a **Dual-Layer Locking Strategy**.

## 🚀 Tech Stack
- **Core:** Java 17, Spring Boot 3.4
- **Database:** PostgreSQL (Optimized with HikariCP Connection Pooling)
- **Caching & Locking:** Redis (Distributed Locks with TTL)
- **Documentation:** OpenAPI (Swagger UI)
- **Testing:** JUnit 5 (CountDownLatch for high-concurrency simulation)
- **Containerization:** Docker & Docker Compose

## 🏗️ Architecture
The system processes booking requests through a two-step concurrency control mechanism to ensure that **exactly one user** can book a specific seat, even if thousands try simultaneously.

### 1. Layer 1: Redis Distributed Lock (The Gatekeeper)
* **Mechanism:** Uses `SETNX` (Set if Not Exists) with a 5-second TTL (Time-To-Live).
* **Purpose:** Acts as a first line of defense. It rejects duplicate requests for the same ticket *before* they even hit the database.
* **Fault Tolerance:** The TTL ensures that if the application server crashes while holding a lock, the lock automatically expires, preventing deadlocks.

### 2. Layer 2: Database Optimistic Locking (The Final Guard)
* **Mechanism:** Uses JPA `@Version` annotation on the `Ticket` entity.
* **Purpose:** Ensures atomic consistency at the database level. If two requests slip past Redis (rare but possible during cache eviction), the database rejects the second write because the version number will not match.

## 🛠️ How to Run

### Prerequisites
* Docker Desktop (Running)
* Java 17+

### 1. Start Infrastructure
Spin up PostgreSQL and Redis containers:
```bash
docker-compose up -d