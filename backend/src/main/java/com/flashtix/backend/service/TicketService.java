package com.flashtix.backend.service;

import com.flashtix.backend.entity.Ticket;
import com.flashtix.backend.exception.TicketBookingException;
import com.flashtix.backend.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // --- STEP 1: DISTRIBUTED LOCK (REDIS) ---
    public Ticket bookTicket(Long ticketId, Long userId) {
        String lockKey = "ticket_lock:" + ticketId;

        // 1. Acquire Lock with TTL (5 seconds)
        // This prevents "deadlocks" if the server crashes while holding a lock.
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey, 
                String.valueOf(userId), 
                5, 
                TimeUnit.SECONDS
        );

        if (Boolean.FALSE.equals(acquired)) {
            throw new TicketBookingException("Too many requests! Please try again.");
        }

        try {
            return processBookingInDatabase(ticketId, userId);
        } finally {
            // Always release the lock
            redisTemplate.delete(lockKey);
        }
    }

    // --- STEP 2: OPTIMISTIC LOCKING (DATABASE) ---
    @Transactional
    public Ticket processBookingInDatabase(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketBookingException("Invalid Ticket ID"));

        if ("SOLD".equals(ticket.getStatus())) {
            throw new TicketBookingException("Sold Out!");
        }

        // Update state
        ticket.setStatus("SOLD");
        ticket.setUserId(userId);

        // Hibernate checks @Version here. 
        // If changed since read, it throws OptimisticLockException automatically.
        return ticketRepository.save(ticket);
    }
}