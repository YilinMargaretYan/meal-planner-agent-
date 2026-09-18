package com.diet.service.meal;

import com.diet.exception.DietException;
import com.diet.model.MealItem;
import com.diet.model.MealSearchRequest;
import com.diet.model.MealSearchResult;
import com.diet.model.SemanticRecallResult;
import com.diet.enums.SourceMode;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 餐食检索服务（Orchestrator 推荐流水线第一层）。
 * 按 sourceMode、userId、slots 从 DB 召回候选，不负责最终重排和 excludeMealIds 过滤。
 */
@Service
public class MealSearchService {

    /** Enough choices for the response agent without broadening an exact result. */
    private static final int MIN_STRICT_CANDIDATES = 3;

    /** 底层餐食服务，封装 MyBatis JSON_OVERLAPS 检索。 */
    private final MealService mealService;
    private final SemanticMealSearchService semanticMealSearchService;

    /** 构造器注入 MealService。 */
    public MealSearchService(MealService mealService, SemanticMealSearchService semanticMealSearchService) {
        this.mealService = mealService;
        this.semanticMealSearchService = semanticMealSearchService;
    }

    /**
     * 执行数据源隔离检索。
     * 由 Orchestrator#completeRecommendation 调用；excludeMealIds 在 MealRankService 层过滤。
     */
    public MealSearchResult search(MealSearchRequest request) {
        return search(request, null);
    }

    /**
     * Adds semantic candidates only when exact tag recall is sparse. The
     * semantic service receives meals already restricted to the selected data
     * source, so it cannot cross PUBLIC and PERSONAL boundaries.
     */
    public MealSearchResult search(MealSearchRequest request, String userQuery) {
        // 请求体或 sourceMode 为空时抛异常
        if (request == null || request.sourceMode() == null) {
            throw new DietException("sourceMode is required.");
        }

        // PERSONAL 模式必须提供 userId，否则无法查个人库
        if (request.sourceMode() == SourceMode.PERSONAL && request.userId() == null) {
            throw new DietException("PERSONAL mode requires a userId.");
        }

        // First preserve the original all-active-slot JSON_OVERLAPS retrieval.
        List<MealItem> strict = mealService.search(request.sourceMode(), request.userId(), request.slots());
        if (strict.size() >= MIN_STRICT_CANDIDATES) {
            return new MealSearchResult(strict, strict.size(), false, 0, false);
        }

        // Sparse multi-slot requests use semantic search over the same source
        // and then retain the original fallback recall as a final safety net.
        List<MealItem> fallback = mealService.searchFallback(request.sourceMode(), request.userId());
        SemanticRecallResult semantic = semanticMealSearchService.search(userQuery, request.slots(), fallback);
        LinkedHashMap<Long, MealItem> merged = new LinkedHashMap<>();
        strict.forEach(item -> merged.put(item.id(), item));
        semantic.candidates().forEach(item -> merged.merge(item.id(), item,
                (existing, incoming) -> incoming.matchScore() > existing.matchScore() ? incoming : existing));
        fallback.forEach(item -> merged.putIfAbsent(item.id(), item));
        return new MealSearchResult(List.copyOf(merged.values()), strict.size(), true,
                semantic.candidates().size(), semantic.used());
    }
}
