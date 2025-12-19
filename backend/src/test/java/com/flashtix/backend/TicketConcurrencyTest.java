package com.flashtix.backend;

import com.flashtix.backend.entity.Ticket;
import com.flashtix.backend.repository.TicketRepository;
import com.flashtix.backend.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TicketConcurrencyTest {

    private static final Logger logger = LoggerFactory.getLogger(TicketConcurrencyTest.class);

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    // --- FIX: Clean the DB before running the test ---
    @BeforeEach
    public void setup() {
        ticketRepository.deleteAll();
    }

    @Test
    public void testConcurrency() throws InterruptedException {
        // 1. Setup
        Ticket t = new Ticket();
        t.setSeatNumber("VIP-TEST-1");
        t.setStatus("AVAILABLE");
        ticketRepository.save(t);
        Long ticketId = t.getId();

        // 2. The Load: 500 threads
        int numberOfThreads = 500;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            long userId = 1000 + i;
            executor.submit(() -> {
                try {
                    latch.await(); // Wait for start signal
                    ticketService.bookTicket(ticketId, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        // 3. Start!
        latch.countDown();
        Thread.sleep(3000); // Allow time for threads to complete
        executor.shutdown();

        // 4. Verify
        logger.info("Total Requests: {}", numberOfThreads);
        logger.info("Success: {}", successCount.get());
        logger.info("Failed: {}", failCount.get());

        assertEquals(1, successCount.get(), "More than one ticket was sold!");
        assertEquals(numberOfThreads - 1, failCount.get(), "Fail count mismatch");
        
        Ticket updatedTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals("SOLD", updatedTicket.getStatus());
    }
}