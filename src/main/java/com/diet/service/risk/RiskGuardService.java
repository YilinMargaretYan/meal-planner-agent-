package com.diet.service.risk;

import com.diet.model.RiskGuardResult;
import com.diet.enums.Intent;
import com.diet.model.RecommendResult;
import com.diet.model.ResponseResult;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 营养与健康风险守卫。
 * 在 Orchestrator#completeRecommendation 中，LLM 生成回复后做最后一道合规检查。
 */
@Component
public class RiskGuardService {

    /**
     * 检查用户输入 + 最终回复是否含健康风险表述。
     * 命中任一规则返回 block，Orchestrator 用 conservativeMessage 替换 speechText。
     */
    public RiskGuardResult check(String userInput, Intent intent, RecommendResult recommendResult, ResponseResult responseResult) {
        List<String> reasons = new ArrayList<>();
        // 拼接用户原文和助手回复，统一扫描关键词
        String allText = (userInput == null ? "" : userInput) + " " + (responseResult == null ? "" : responseResult.speechText());
        // 规则 1：意图层已识别为 HEALTH_RISK
        if (intent == Intent.HEALTH_RISK) {
            reasons.add("HEALTH_RISK intent detected");
        }
        // 规则 2：医疗诊断/治疗/处方承诺
        if (containsAny(allText, "cure", "treatment", "diagnosis", "medication", "prescription")) {
            reasons.add("medical diagnosis or treatment claim detected");
        }
        // 规则 3：极端节食建议
        if (containsAny(allText, "starvation", "do not eat", "only water", "extreme diet")) {
            reasons.add("extreme dieting advice detected");
        }
        // 规则 4：绝对化健康/减肥承诺
        if (containsAny(allText, "guaranteed", "will make you lose weight", "healthiest", "guarantee weight loss")) {
            reasons.add("absolute health claim detected");
        }
        // 规则 5：特殊人群或慢病
        if (containsAny(allText, "pregnant", "diabetes", "high blood pressure", "minor", "child")) {
            reasons.add("special population or chronic-condition risk detected");
        }
        // 无命中规则 → 通过
        if (reasons.isEmpty()) {
            return RiskGuardResult.pass();
        }
        // 有命中 → 拦截，返回 reasons 和 conservativeMessage 作为 rewriteSuggestion
        return RiskGuardResult.block(reasons, conservativeMessage());
    }

    /** 高风险场景的保守固定提示文案。 */
    public String conservativeMessage() {
        return "This question involves a health or medical concern, so I cannot diagnose or provide treatment advice. For general meals, consider balanced options and avoid extremes. If symptoms are significant or you have a medical condition, are pregnant, or have other special needs, consult a clinician or registered dietitian.";
    }

    /** 判断 text 是否包含 keywords 中任一子串。 */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

