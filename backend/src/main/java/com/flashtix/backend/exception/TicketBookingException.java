package com.flashtix.backend.exception;

public class TicketBookingException extends RuntimeException {
    public TicketBookingException(String message) {
        super(message);
    }
}