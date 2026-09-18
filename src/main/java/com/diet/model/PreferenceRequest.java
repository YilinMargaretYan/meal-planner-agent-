package com.diet.model;

import com.diet.enums.PreferenceType;

import java.time.LocalDateTime;

/** API input for an explicit preference; clients may set an expiry for temporary preferences. */
public record PreferenceRequest(
        String slotName,
        String optionValue,
        PreferenceType preferenceType,
        Double confidence,
        LocalDateTime expiresAt
) {
}
