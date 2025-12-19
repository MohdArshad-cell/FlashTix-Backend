package com.flashtix.backend.service;

import com.flashtix.backend.entity.Ticket;
import com.flashtix.backend.exception.TicketBookingException;
import com.flashtix.backend.repository.TicketRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class TicketService {

    private static final String UNLOCK_LUA_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    private static final long LOCK_TTL_SECONDS = 5;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // --- METRICS ---
    private final Counter soldOutCounter;

    public TicketService(MeterRegistry registry) {
        // This registers the metric "flashtix.sold.out" in Prometheus
        this.soldOutCounter = Counter.builder("flashtix.sold.out")
                .description("Count of booking attempts rejected because ticket was already sold")
                .register(registry);
    }

    public Ticket bookTicket(Long ticketId, Long userId) {
        String lockKey = "ticket_lock:" + ticketId;
        String userIdStr = String.valueOf(userId);

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, userIdStr, LOCK_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(acquired)) {
            // OPTIONAL: You could count "Lock Contention" here too if you wanted
            throw new TicketBookingException("Too many requests! Please try again.");
        }

        try {
            return processBookingInDatabase(ticketId, userId);
        } finally {
            RedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);
            redisTemplate.execute(script, Collections.singletonList(lockKey), userIdStr);
        }
    }

    @Transactional
    public Ticket processBookingInDatabase(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketBookingException("Invalid Ticket ID"));

        if ("SOLD".equals(ticket.getStatus())) {
            // --- CRITICAL: This line makes the graph spike ---
            soldOutCounter.increment(); 
            throw new TicketBookingException("Sold Out!");
        }

        ticket.setStatus("SOLD");
        ticket.setUserId(userId);
        return ticketRepository.save(ticket);
    }
}