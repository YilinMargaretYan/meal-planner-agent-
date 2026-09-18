package com.diet.service.meal;

import com.diet.model.MealItem;
import com.diet.model.MealRankRequest;
import com.diet.model.SlotBundle;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 餐食重排服务（Orchestrator 推荐流水线第二层）。
 * 消费 slots、excludeMealIds，对检索候选二次打分排序。
 */
@Service
public class MealRankService {

    /**
     * 在search检索时,只要用户输入的槽位信息有匹配有交集就会返回
     * 但是rank排序,是比较用户输入的槽位信息是否更大程度的得到满足
     * 执行重排并返回最多 10 个候选。
     * 由 Orchestrator#completeRecommendation 在 MEAL_SEARCHED 之后调用。
     */
    public List<MealItem> rank(MealRankRequest request) {
        // 将 excludeMealIds 转为 HashSet，便于 O(1) 查找
        Set<Long> excludeIds = new HashSet<>(request.excludeMealIds() == null ? List.of() : request.excludeMealIds());
        return request.candidates().stream()
                .filter(item -> item != null && !excludeIds.contains(item.id()))  // 过滤 null 和需排除的 ID
                .filter(item -> !matchesAvoidPreference(item, request.avoidSlots(), request.slots()))
                .map(item -> withRankScore(item, request.slots())) // 计算排序分数
                .sorted(Comparator.comparingDouble((MealItem item) -> item.matchScore()).reversed()) // 按分数降序
                .limit(10)   // 最多返回 10 条
                .toList();
    }

    /**
     * 计算重排后的归一化分数并返回新 MealItem（matchScore 替换为 finalScore）。
     */
    private MealItem withRankScore(MealItem item, SlotBundle query) {
        double slotScore = slotScore(item.slots(), query);           // 槽位命中分 [0,1]
        double semanticScore = clamp(item.matchScore());
        // Strict-tag candidates retain their exact-match score. For semantic
        // candidates, blend semantic relevance in only when slots are present;
        // this avoids losing a natural-language match to an arbitrary tie.
        double finalScore = semanticScore == 0 ? slotScore
                : hasActiveSlots(query) ? 0.8 * slotScore + 0.2 * semanticScore : semanticScore;
        return new MealItem(item.id(), item.sourceType(), item.ownerUserId(), item.name(), item.slots(), finalScore);
    }

    private boolean hasActiveSlots(SlotBundle slots) {
        SlotBundle safeSlots = slots == null ? SlotBundle.empty() : slots;
        return !safeSlots.mealTime().isEmpty() || !safeSlots.mood().isEmpty() || !safeSlots.scene().isEmpty()
                || !safeSlots.healthGoal().isEmpty() || !safeSlots.cuisine().isEmpty()
                || !safeSlots.taste().isEmpty() || !safeSlots.convenience().isEmpty();
    }

    /**
     * Computes the average overlap across only the dimensions the user actually
     * supplied. The former fixed seven-dimension denominator artificially
     * penalized short but valid requests such as “a quick lunch”.
     */
    private double slotScore(SlotBundle item, SlotBundle query) {
        SlotBundle safeQuery = query == null ? SlotBundle.empty() : query;
        List<List<String>> itemDimensions = List.of(
                item.mealTime(), item.mood(), item.scene(), item.healthGoal(),
                item.cuisine(), item.taste(), item.convenience());
        List<List<String>> queryDimensions = List.of(
                safeQuery.mealTime(), safeQuery.mood(), safeQuery.scene(), safeQuery.healthGoal(),
                safeQuery.cuisine(), safeQuery.taste(), safeQuery.convenience());
        double total = 0;
        int activeDimensions = 0;
        for (int index = 0; index < queryDimensions.size(); index++) {
            List<String> requested = queryDimensions.get(index);
            if (requested == null || requested.isEmpty()) {
                continue;
            }
            total += overlap(itemDimensions.get(index), requested);
            activeDimensions++;
        }
        return activeDimensions == 0 ? 0 : clamp(total / activeDimensions);
    }

    /**
     * Avoid preferences apply only when the user did not explicitly request a
     * value in that dimension in the current working context. This keeps saved
     * preferences helpful without making them impossible to override.
     */
    private boolean matchesAvoidPreference(MealItem item, SlotBundle avoid, SlotBundle query) {
        SlotBundle safeAvoid = avoid == null ? SlotBundle.empty() : avoid;
        SlotBundle safeQuery = query == null ? SlotBundle.empty() : query;
        SlotBundle itemSlots = item.slots() == null ? SlotBundle.empty() : item.slots();
        return matchesAny(itemSlots.mealTime(), safeAvoid.mealTime(), safeQuery.mealTime())
                || matchesAny(itemSlots.mood(), safeAvoid.mood(), safeQuery.mood())
                || matchesAny(itemSlots.scene(), safeAvoid.scene(), safeQuery.scene())
                || matchesAny(itemSlots.healthGoal(), safeAvoid.healthGoal(), safeQuery.healthGoal())
                || matchesAny(itemSlots.cuisine(), safeAvoid.cuisine(), safeQuery.cuisine())
                || matchesAny(itemSlots.taste(), safeAvoid.taste(), safeQuery.taste())
                || matchesAny(itemSlots.convenience(), safeAvoid.convenience(), safeQuery.convenience());
    }

    private boolean matchesAny(List<String> itemValues, List<String> avoidValues, List<String> requestedValues) {
        if (avoidValues == null || avoidValues.isEmpty() || (requestedValues != null && !requestedValues.isEmpty())) {
            return false;
        }
        Set<String> itemSet = Set.copyOf(itemValues == null ? List.of() : itemValues);
        return avoidValues.stream().anyMatch(itemSet::contains);
    }

    /** 计算 queryValues 中有多少标签出现在 itemValues 中，返回命中比例。 */
    private double overlap(List<String> itemValues, List<String> queryValues) {
        if (queryValues == null || queryValues.isEmpty()) {
            return 0; // 查询侧该维度为空时不计分
        }
        Set<String> itemSet = Set.copyOf(itemValues == null ? List.of() : itemValues);
        long hits = queryValues.stream().filter(itemSet::contains).count();
        // hits * 1.0 / queryValues.size()  即  命中的用户标签数 / 用户查询标签总数
        // 例如 鸡胸肉的health_Goal有[清淡，高蛋白] 猪肘的health_Goal有[高蛋白]，用户的输入的health_Goal是[清淡，高蛋白]
        // 那这里 queryValues就是[清淡，高蛋白]
        // 鸡胸肉的 hits = 2   猪肘的 hits = 1
        // 于是最终得分，鸡胸肉的 score = 2 / 2 = 1,  猪肘的 score = 1 / 2 = 0.5 分
        // 排序的规则就是看 谁更能满足用户的需求
        return hits * 1.0 / queryValues.size();
    }

    /** 将分数约束在 [0, 1] 区间。 */
    private double clamp(double score) {
        return Math.max(0, Math.min(1, score));
    }
}
