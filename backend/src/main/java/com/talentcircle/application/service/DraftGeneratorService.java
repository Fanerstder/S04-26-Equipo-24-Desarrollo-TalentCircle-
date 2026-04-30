package com.talentcircle.application.service;

import com.talentcircle.domain.model.Draft;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.DraftGeneratorUseCase;
import com.talentcircle.domain.port.out.AiAnalysisRepository;
import com.talentcircle.domain.port.out.DraftRepository;
import com.talentcircle.domain.port.out.LlmClientPort;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DraftGeneratorService implements DraftGeneratorUseCase {

    private final WeeklyExecutionRepository executionRepository;
    private final AiAnalysisRepository analysisRepository;
    private final DraftRepository draftRepository;
    private final LlmClientPort llmClient;

    public DraftGeneratorService(WeeklyExecutionRepository executionRepository,
                                AiAnalysisRepository analysisRepository,
                                DraftRepository draftRepository,
                                LlmClientPort llmClient) {
        this.executionRepository = executionRepository;
        this.analysisRepository = analysisRepository;
        this.draftRepository = draftRepository;
        this.llmClient = llmClient;
    }

    @Override
    public List<Draft> generateDrafts(String executionId) {
        WeeklyExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));

        // Get AI analysis
        // In real impl: fetch from analysisRepository.findByExecutionId(executionId)

        // Generate drafts for each channel
        Draft newsletter = generateDraftForChannel(execution, Draft.Channel.NEWSLETTER, "newsletter prompt");
        Draft linkedin = generateDraftForChannel(execution, Draft.Channel.LINKEDIN, "linkedin prompt");
        Draft twitter = generateDraftForChannel(execution, Draft.Channel.TWITTER, "twitter prompt");

        return List.of(
                draftRepository.save(newsletter),
                draftRepository.save(linkedin),
                draftRepository.save(twitter)
        );
    }

    private Draft generateDraftForChannel(WeeklyExecution execution, Draft.Channel channel, String promptTemplate) {
        Draft draft = new Draft();
        draft.setExecution(execution);
        draft.setChannel(channel);
        draft.setStatus(Draft.DraftStatus.PENDING);

        // Call LLM to generate content
        String draftContent = llmClient.generateDraft("analysis json", channel.name(), promptTemplate);

        // Validate length constraints
        if (channel == Draft.Channel.TWITTER && draftContent.length() > 280) {
            draftContent = draftContent.substring(0, 280);
        }

        draft.setContent(draftContent);
        return draft;
    }
}
