package com.talentcircle.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analyses")
public class AiAnalysis extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id")
    private WeeklyExecution execution;

    @Column(name = "top_topics", columnDefinition = "TEXT")
    private String topTopics; // JSONB for topics

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    @Column(name = "relevance_scores", columnDefinition = "TEXT")
    private String relevanceScores; // JSONB for scores

    @Column(name = "llm_provider")
    private String llmProvider;

    @Column(name = "prompt_tokens")
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens")
    private Integer completionTokens = 0;

    // Getters and Setters
    public WeeklyExecution getExecution() {
        return execution;
    }

    public void setExecution(WeeklyExecution execution) {
        this.execution = execution;
    }

    public String getTopTopics() {
        return topTopics;
    }

    public void setTopTopics(String topTopics) {
        this.topTopics = topTopics;
    }

    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }

    public String getRelevanceScores() {
        return relevanceScores;
    }

    public void setRelevanceScores(String relevanceScores) {
        this.relevanceScores = relevanceScores;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }
}
