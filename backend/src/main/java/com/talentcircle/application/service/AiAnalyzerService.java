package com.talentcircle.application.service;

import com.talentcircle.domain.model.AiAnalysis;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.AiAnalyzerUseCase;
import com.talentcircle.domain.port.out.AiAnalysisRepository;
import com.talentcircle.domain.port.out.LlmClientPort;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional`
public class AiAnalyzerService implements AiAnalyzerUseCase {

    private final WeeklyExecutionRepository executionRepository;
    private final AiAnalysisRepository analysisRepository;
    private final LlmClientPort llmClient;

    public AiAnalyzerService(WeeklyExecutionRepository executionRepository,
                          AiAnalysisRepository analysisRepository,
                          LlmClientPort llmClient) {
        this.executionRepository = executionRepository;
        this.analysisRepository = analysisRepository;
        this.llmClient = llmClient;
    }

    @Override`
    public AiAnalysis analyzeActivity(String executionId, String promptTemplate) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));

        // Get activities
        List<?> activities = execution.getActivities(); // In real impl, get from repo

        // Call LLM
        AiAnalysis analysis = llmClient.analyzeActivity(activities, promptTemplate);

        // Link to execution
        analysis.setExecution(execution);
        return analysisRepository.save(analysis);
    }
}
