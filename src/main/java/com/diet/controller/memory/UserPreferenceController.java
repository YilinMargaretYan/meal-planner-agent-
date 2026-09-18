package com.diet.controller.memory;

import com.diet.constants.DietConstants;
import com.diet.model.PreferenceRequest;
import com.diet.model.PreferenceResponse;
import com.diet.service.memory.UserPreferenceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** User-controlled long-term preference memory endpoints. */
@RestController
@RequestMapping("/api/v1/diet/preferences")
public class UserPreferenceController {
    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping
    public List<PreferenceResponse> list(@RequestHeader(value = DietConstants.USER_ID, defaultValue = "1") Long userId) {
        return userPreferenceService.findActive(userId).stream().map(PreferenceResponse::from).toList();
    }

    @PostMapping
    public PreferenceResponse save(
            @RequestHeader(value = DietConstants.USER_ID, defaultValue = "1") Long userId,
            @RequestBody PreferenceRequest request) {
        return PreferenceResponse.from(userPreferenceService.save(userId, request));
    }

    @DeleteMapping("/{preferenceId}")
    public void delete(
            @RequestHeader(value = DietConstants.USER_ID, defaultValue = "1") Long userId,
            @PathVariable Long preferenceId) {
        userPreferenceService.delete(userId, preferenceId);
    }
}
