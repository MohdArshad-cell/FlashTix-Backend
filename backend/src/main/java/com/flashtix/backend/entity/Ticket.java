package com.flashtix.backend.entity;

import jakarta.persistence.*;
import lombok.Data; // Auto-generates getters, setters, toString

@Entity
@Table(name = "tickets")
@Data
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String seatNumber;

    private String status; // "AVAILABLE" or "SOLD"
    private Long userId;

    @Version
    private Long version; 
    
    // No manual getters/setters needed anymore!
}