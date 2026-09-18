package com.diet.model;

import com.diet.enums.PreferenceType;

import java.time.LocalDateTime;

/** Safe API representation of a saved preference. */
public record PreferenceResponse(
        Long id,
        String slotName,
        String optionValue,
        PreferenceType preferenceType,
        Double confidence,
        LocalDateTime expiresAt
) {
    public static PreferenceResponse from(UserPreferenceRow row) {
        return new PreferenceResponse(
                row.getId(),
                row.getSlotName(),
                row.getOptionValue(),
                PreferenceType.valueOf(row.getPreferenceType()),
                row.getConfidence(),
                row.getExpiresAt()
        );
    }
}
