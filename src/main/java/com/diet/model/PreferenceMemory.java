package com.diet.model;

import com.diet.enums.PreferenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * A compact memory view supplied to the deterministic retrieval path. PREFER
 * values fill missing session slots, while AVOID values are rank-time filters.
 */
public record PreferenceMemory(SlotBundle preferredSlots, SlotBundle avoidSlots) {
    public static PreferenceMemory empty() {
        return new PreferenceMemory(SlotBundle.empty(), SlotBundle.empty());
    }

    public static PreferenceMemory fromRows(List<UserPreferenceRow> rows) {
        List<String> preferMealTime = new ArrayList<>();
        List<String> preferMood = new ArrayList<>();
        List<String> preferScene = new ArrayList<>();
        List<String> preferHealthGoal = new ArrayList<>();
        List<String> preferCuisine = new ArrayList<>();
        List<String> preferTaste = new ArrayList<>();
        List<String> preferConvenience = new ArrayList<>();
        List<String> avoidMealTime = new ArrayList<>();
        List<String> avoidMood = new ArrayList<>();
        List<String> avoidScene = new ArrayList<>();
        List<String> avoidHealthGoal = new ArrayList<>();
        List<String> avoidCuisine = new ArrayList<>();
        List<String> avoidTaste = new ArrayList<>();
        List<String> avoidConvenience = new ArrayList<>();

        for (UserPreferenceRow row : rows == null ? List.<UserPreferenceRow>of() : rows) {
            if (row == null || row.getSlotName() == null || row.getOptionValue() == null || row.getPreferenceType() == null) {
                continue;
            }
            List<String> target = target(row.getPreferenceType(), row.getSlotName(),
                    preferMealTime, preferMood, preferScene, preferHealthGoal, preferCuisine, preferTaste, preferConvenience,
                    avoidMealTime, avoidMood, avoidScene, avoidHealthGoal, avoidCuisine, avoidTaste, avoidConvenience);
            if (target != null) {
                target.add(row.getOptionValue());
            }
        }
        return new PreferenceMemory(
                new SlotBundle(preferMealTime, preferMood, preferScene, preferHealthGoal, preferCuisine, preferTaste, preferConvenience),
                new SlotBundle(avoidMealTime, avoidMood, avoidScene, avoidHealthGoal, avoidCuisine, avoidTaste, avoidConvenience)
        );
    }

    private static List<String> target(
            String rawType,
            String slotName,
            List<String> preferMealTime, List<String> preferMood, List<String> preferScene, List<String> preferHealthGoal,
            List<String> preferCuisine, List<String> preferTaste, List<String> preferConvenience,
            List<String> avoidMealTime, List<String> avoidMood, List<String> avoidScene, List<String> avoidHealthGoal,
            List<String> avoidCuisine, List<String> avoidTaste, List<String> avoidConvenience) {
        PreferenceType type;
        try {
            type = PreferenceType.valueOf(rawType);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return switch (slotName) {
            case "mealTime" -> type == PreferenceType.PREFER ? preferMealTime : avoidMealTime;
            case "mood" -> type == PreferenceType.PREFER ? preferMood : avoidMood;
            case "scene" -> type == PreferenceType.PREFER ? preferScene : avoidScene;
            case "healthGoal" -> type == PreferenceType.PREFER ? preferHealthGoal : avoidHealthGoal;
            case "cuisine" -> type == PreferenceType.PREFER ? preferCuisine : avoidCuisine;
            case "taste" -> type == PreferenceType.PREFER ? preferTaste : avoidTaste;
            case "convenience" -> type == PreferenceType.PREFER ? preferConvenience : avoidConvenience;
            default -> null;
        };
    }
}
