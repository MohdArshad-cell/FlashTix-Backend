![Build Status](https://github.com/MohdArshad-cell/FlashTix-Backend/actions/workflows/maven.yml/badge.svg)

```markdown
# ⚡ FlashTix - High-Concurrency Ticketing Engine

FlashTix is an enterprise-grade backend system designed to handle massive traffic surges (e.g., flash sales) while guaranteeing strict data consistency. It prevents race conditions and overbooking using a **Dual-Layer Locking Strategy** (Redis Distributed Locks + Database Optimistic Locking).

## 🚀 Tech Stack
- **Core:** Java 17, Spring Boot 3.4
- **Database:** PostgreSQL (Optimized with HikariCP Connection Pooling)
- **Caching & Locking:** Redis (Distributed Locks with TTL)
- **Testing:** JUnit 5 & Java Concurrency (ExecutorService, CountDownLatch)
- **Documentation:** OpenAPI (Swagger UI)
- **Containerization:** Docker Support

## 🏗️ System Architecture: The "Two-Layer Defense"

The system processes booking requests through a rigorous two-step concurrency control mechanism to ensure that **exactly one user** can book a specific seat, even if 500+ users try simultaneously.

```mermaid
graph TD
    User[User Request] -->|POST /book| LB[Load Balancer]
    LB --> API[Booking Service]
    
    subgraph "Layer 1: Redis Gatekeeper"
        API -->|SETNX lock:ticket:{id}| Redis{Acquire Lock?}
        Redis -- No --> Reject[409 Conflict: Too Many Requests]
    end
    
    subgraph "Layer 2: Database Final Guard"
        Redis -- Yes --> DB[PostgreSQL Transaction]
        DB -->|Check @Version| Verify{Version Match?}
        Verify -- No --> Rollback[OptimisticLockException]
        Verify -- Yes --> Commit[Success: Ticket Booked]
    end

```

###1. Layer 1: Redis Distributed Lock (The Gatekeeper)* **Code Reference:** `TicketService.java`
* **Mechanism:** Uses `SETNX` (Set if Not Exists) with a 5-second TTL.
* **Why:** Acts as a first line of defense to reduce database pressure. It creates a "queue-like" effect where only one request per ticket processes at a time.
* **Fault Tolerance:** The TTL ensures that if the server crashes while holding a lock, it automatically expires to prevent deadlocks.

###2. Layer 2: Database Optimistic Locking (The Final Guard)* **Code Reference:** `Ticket.java` (`@Version` annotation)
* **Mechanism:** Hibernate/JPA validates the version number before committing.
* **Why:** Ensures atomic consistency at the persistent storage level. If a request somehow bypasses Redis (e.g., during cache eviction or split-brain), the database rejects the write because the version numbers won't match.

##🧪 "Proof of Work" - The Concurrency Stress TestI didn't just write the code; I proved it works under fire. The project includes a dedicated stress test (`TicketConcurrencyTest.java`) that simulates a real-world attack.

###Test Scenario:* **Threads:** 500 Concurrent Threads (simulating 500 users clicking "Buy" at the exact same millisecond).
* **Target:** 1 Single Ticket (`VIP-TEST-1`).
* **Tool:** `ExecutorService` and `CountDownLatch`.

###The Code (`TicketConcurrencyTest.java`):```java
// Simulating 500 users attacking 1 ticket
ExecutorService executor = Executors.newFixedThreadPool(500);
CountDownLatch latch = new CountDownLatch(1);

for (int i = 0; i < 500; i++) {
    executor.submit(() -> {
        latch.await(); // Hold all threads until signal
        ticketService.bookTicket(ticketId, userId); // FIRE!
    });
}

```

###📉 Results| Metric | Count |
| --- | --- |
| **Total Requests** | 500 |
| **Successful Bookings** | **1** (Exactly) |
| **Failed Requests** | **499** (Correctly Rejected) |
| **Data Integrity** | **100%** |

##🛠️ How to Run###1. Start InfrastructureMake sure you have Docker installed. Spin up the Database and Cache:

```bash
docker-compose up -d

```

*(This starts PostgreSQL on port 5432 and Redis on port 6379)*

###2. Run the Application```bash
./mvnw spring-boot:run

```

###3. API DocumentationOnce running, explore the endpoints via Swagger UI:
👉 **[http://localhost:8080/swagger-ui.html](https://www.google.com/search?q=http://localhost:8080/swagger-ui.html)**

##📝 Key Configuration (`application.properties`)To handle high load, we tuned the HikariCP connection pool:

```properties
# High Concurrency Tuning
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.connection-timeout=30000

```

##👨‍💻 Author**Mohd Arshad**
Backend Engineer | [LinkedIn](https://www.google.com/search?q=https://www.linkedin.com/in/mohd-arshad-156227314/)

```

```
