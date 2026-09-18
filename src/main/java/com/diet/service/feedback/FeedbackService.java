package com.diet.service.feedback;

import com.diet.exception.DietException;
import com.diet.mapper.FeedbackMapper;
import com.diet.model.FeedbackRequest;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {
    private final FeedbackMapper feedbackMapper;

    public FeedbackService(FeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    public void save(Long userId, FeedbackRequest request) {
        if (request == null || request.sessionId() == null || request.sessionId().isBlank()) {
            throw new DietException("Feedback sessionId is required.");
        }
        if (request.action() == null || request.action().isBlank()) {
            throw new DietException("Feedback action is required.");
        }
        feedbackMapper.insert(
                userId,
                request.sessionId(),
                request.itemId(),
                request.action(),
                request.rating(),
                request.reason()
        );
    }
}
