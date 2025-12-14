package com.flashtix.backend.controller;

import com.flashtix.backend.entity.Ticket;
import com.flashtix.backend.repository.TicketRepository;
import com.flashtix.backend.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class BookingController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @PostMapping("/seed")
    public ResponseEntity<String> seedTickets() {
        if (ticketRepository.count() > 0) {
            return ResponseEntity.ok("Database already has tickets.");
        }
        for (int i = 1; i <= 100; i++) {
            Ticket t = new Ticket();
            t.setSeatNumber("Seat-" + i);
            t.setStatus("AVAILABLE");
            ticketRepository.save(t);
        }
        return ResponseEntity.ok("Created 100 Seats!");
    }

    @Operation(summary = "Book a ticket", description = "Handles high concurrency using Redis Dist. Locks + Optimistic Locking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking Successful"),
        @ApiResponse(responseCode = "409", description = "Conflict: Ticket sold or locked")
    })
    @PostMapping("/book")
    public ResponseEntity<Ticket> bookTicket(@RequestParam Long ticketId, @RequestParam Long userId) {
        Ticket bookedTicket = ticketService.bookTicket(ticketId, userId);
        return ResponseEntity.ok(bookedTicket);
    }
}