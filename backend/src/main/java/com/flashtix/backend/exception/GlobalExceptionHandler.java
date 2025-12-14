package com.flashtix.backend.exception;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles our custom logic errors (Sold Out, Invalid ID, Redis Lock contention)
    @ExceptionHandler(TicketBookingException.class)
    public ResponseEntity<Map<String, String>> handleBookingException(TicketBookingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "status", "error",
                        "message", ex.getMessage()
                ));
    }

    // Handles the Database Race Condition (Optimistic Locking failure)
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(OptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "status", "error",
                        "message", "Race Condition Detected! The ticket was modified by another user."
                ));
    }
}