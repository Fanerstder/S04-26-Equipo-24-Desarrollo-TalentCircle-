package com.talentcircle.application.service;

import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.PipelineOrchestratorUseCase;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class PipelineOrchestratorService implements PipelineOrchestratorUseCase {

    private final WeeklyExecutionRepository executionRepository;
    private final com.talentcircle.domain.port.in.CommunityCollectorUseCase communityCollectorUseCase;
    private final com.talentcircle.domain.port.in.AiAnalyzerUseCase aiAnalyzerUseCase;
    private final com.talentcircle.domain.port.in.DraftGeneratorUseCase draftGeneratorUseCase;

    public PipelineOrchestratorService(
            WeeklyExecutionRepository executionRepository,
            com.talentcircle.domain.port.in.CommunityCollectorUseCase communityCollectorUseCase,
            com.talentcircle.domain.port.in.AiAnalyzerUseCase aiAnalyzerUseCase,
            com.talentcircle.domain.port.in.DraftGeneratorUseCase draftGeneratorUseCase) {
        this.executionRepository = executionRepository;
        this.communityCollectorUseCase = communityCollectorUseCase;
        this.aiAnalyzerUseCase = aiAnalyzerUseCase;
        this.draftGeneratorUseCase = draftGeneratorUseCase;
    }

    @Override
    public void runWeeklyPipeline(String triggeredBy) {
        WeeklyExecution execution = new WeeklyExecution();
        LocalDate now = LocalDate.now();
        execution.setWeekStart(now.minusDays(now.getDayOfWeek().getValue() - 1)); // Monday
        execution.setWeekEnd(execution.getWeekStart().plusDays(6)); // Sunday
        execution.setStatus(WeeklyExecution.ExecutionStatus.RUNNING);
        execution.setStartedAt(java.time.LocalDateTime.now());
        execution.setTriggeredBy(triggeredBy);

        execution = executionRepository.save(execution);

        try {
            // Step 1: Collect activities
            // communityCollectorUseCase.collectActivity(execution.getId(), ...);

            // Step 2: Analyze with AI
            // aiAnalyzerUseCase.analyzeActivity(execution.getId(), ...);

            // Step 3: Generate drafts
            // draftGeneratorUseCase.generateDrafts(execution.getId());

            execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
            execution.setCompletedAt(java.time.LocalDateTime.now());
        } catch (Exception e) {
            execution.setStatus(WeeklyExecution.ExecutionStatus.FAILED);
            execution.setCompletedAt(java.time.LocalDateTime.now());
        }

        executionRepository.save(execution);
    }

    @Override
    public void retryFailedStep(String executionId) {
        throw new RuntimeException("Retry not implemented yet");
    }
}
