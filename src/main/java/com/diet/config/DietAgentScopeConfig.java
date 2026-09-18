package com.diet.config;

import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 模型配置。
 */
@Configuration
public class DietAgentScopeConfig {

    /** OpenAI API Key，所有 AgentScope 模型调用都依赖该配置。 */
    @Value("${agentscope.openai.api-key}")
    private String apiKey;

    /** 主模型用于推荐理由和最终应答，默认使用 gpt-5.2。 */
    @Value("${diet.llm.main-model:gpt-5.2}")
    private String mainModelName;

    /** 轻量模型用于意图识别和澄清追问，默认使用 gpt-5.2。 */
    @Value("${diet.llm.light-model:gpt-5.2}")
    private String lightModelName;

    /**
     * 主模型 Bean。
     * RecommendResponseAgent 会优先使用该模型。
     */
    @Bean("DietMainChatModel")
    public Model DietMainChatModel() {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(mainModelName)
                .build();
    }

    /**
     * 轻量模型 Bean。
     * IntentAgent 和 ClarifyAgent 使用它降低延迟和成本。
     */
    @Bean("DietLightChatModel")
    public Model DietLightChatModel() {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(lightModelName)
                .build();
    }
}




