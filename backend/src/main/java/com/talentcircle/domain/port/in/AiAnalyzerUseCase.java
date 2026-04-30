package com.talentcircle.domain.port.in;

import com.talentcircle.domain.model.AiAnalysis;

public interface AiAnalyzerUseCase {
    AiAnalysis analyzeActivity(String executionId, String promptTemplate);
}
