package com.diet.model;

import lombok.Data;

import java.time.LocalDateTime;

/** Persistent, explicit preference record. Raw chat messages are never promoted automatically. */
@Data
public class UserPreferenceRow {
    private Long id;
    private Long userId;
    private String slotName;
    private String optionValue;
    private String preferenceType;
    private Double confidence;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
