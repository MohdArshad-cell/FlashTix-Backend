package com.flashtix.backend;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Disabled;

@Disabled
public class ApiLoadTest {

    @Test
    public void attackRunningServer() throws InterruptedException {
        // 1. Target the RUNNING server
        String url = "http://localhost:8080/api/tickets/book?ticketId=1&userId=";
        
        // 2. Launch 500 threads
        int numberOfThreads = 500;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        HttpClient client = HttpClient.newHttpClient();
        AtomicInteger soldOutCount = new AtomicInteger(0);

        System.out.println("🚀 STARTING ATTACK on localhost:8080...");

        for (int i = 0; i < numberOfThreads; i++) {
            int userId = 1000 + i;
            executor.submit(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url + userId))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    // Count the 409 Conflicts (Sold Out)
                    if (response.statusCode() == 409) {
                        soldOutCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("✅ ATTACK FINISHED. Sold Out Errors: " + soldOutCount.get());
    }
}