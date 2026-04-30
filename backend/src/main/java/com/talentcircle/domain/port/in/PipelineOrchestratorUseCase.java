package com.talentcircle.domain.port.in;

public interface PipelineOrchestratorUseCase {
    void runWeeklyPipeline(String triggeredBy);
    void retryFailedStep(String executionId);
}
