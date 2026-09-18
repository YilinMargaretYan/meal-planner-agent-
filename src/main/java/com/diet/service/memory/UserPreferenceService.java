package com.diet.service.memory;

import com.diet.enums.PreferenceType;
import com.diet.exception.DietException;
import com.diet.mapper.UserPreferenceMapper;
import com.diet.model.PreferenceMemory;
import com.diet.model.PreferenceRequest;
import com.diet.model.UserPreferenceRow;
import com.diet.service.slot.SlotOptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Manages only explicit user preferences. It intentionally never infers durable
 * facts from a single chat turn, avoiding accidental or stale profile memory.
 */
@Service
public class UserPreferenceService {
    private final UserPreferenceMapper userPreferenceMapper;
    private final SlotOptionService slotOptionService;

    public UserPreferenceService(UserPreferenceMapper userPreferenceMapper, SlotOptionService slotOptionService) {
        this.userPreferenceMapper = userPreferenceMapper;
        this.slotOptionService = slotOptionService;
    }

    public List<UserPreferenceRow> findActive(Long userId) {
        return userId == null ? List.of() : userPreferenceMapper.findActiveByUserId(userId);
    }

    public PreferenceMemory memoryFor(Long userId) {
        return PreferenceMemory.fromRows(findActive(userId));
    }

    @Transactional
    public UserPreferenceRow save(Long userId, PreferenceRequest request) {
        if (userId == null || request == null || request.slotName() == null || request.optionValue() == null || request.preferenceType() == null) {
            throw new DietException("Preference information is incomplete.");
        }
        String slotName = request.slotName().trim();
        String optionValue = request.optionValue().trim();
        validateOption(slotName, optionValue);
        if (request.expiresAt() != null && !request.expiresAt().isAfter(LocalDateTime.now())) {
            throw new DietException("The preference expiry time must be in the future.");
        }
        UserPreferenceRow row = new UserPreferenceRow();
        row.setUserId(userId);
        row.setSlotName(slotName);
        row.setOptionValue(optionValue);
        row.setPreferenceType(request.preferenceType().name());
        row.setConfidence(normalizeConfidence(request.confidence()));
        row.setExpiresAt(request.expiresAt());
        userPreferenceMapper.upsert(row);
        return userPreferenceMapper.findByUnique(userId, slotName, optionValue, request.preferenceType().name());
    }

    @Transactional
    public void delete(Long userId, Long preferenceId) {
        if (userId == null || preferenceId == null || userPreferenceMapper.deleteById(preferenceId, userId) == 0) {
            throw new DietException("The preference does not exist or cannot be deleted by this user.");
        }
    }

    private void validateOption(String slotName, String optionValue) {
        Map<String, List<String>> options = slotOptionService.findAllOptions();
        if (!options.containsKey(slotName) || !options.get(slotName).contains(optionValue)) {
            throw new DietException("Preferences must use valid tags from the current tag dictionary.");
        }
    }

    private double normalizeConfidence(Double confidence) {
        double value = confidence == null ? 1.0 : confidence;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
