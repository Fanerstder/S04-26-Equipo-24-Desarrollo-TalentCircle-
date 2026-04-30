package com.talentcircle.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity`
@Table(name = "pipeline_configs")
public class PipelineConfig extends AuditableEntity {

    @Column(name = "llm_provider")
    private String llmProvider;

    @Column(name = "llm_model")
    private String llmModel;

    @Column(name = "newsletter_prompt", columnDefinition = "TEXT")
    private String newsletterPrompt;

    @Column(name = "linkedin_prompt", columnDefinition = "TEXT")
    private String linkedinPrompt;

    @Column(name = "twitter_prompt", columnDefinition = "TEXT")
    private String twitterPrompt;

    @Column(name = "max_items_per_channel")
    private Integer maxItemsPerChannel = 10;

    @Column(name = "schedule_cron")
    private String scheduleCron = "0 18 ? * FRI"; // Friday 18:00

    // Getters and Setters
    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public String getNewsletterPrompt() {
        return newsletterPrompt;
    }

    public void setNewsletterPrompt(String newsletterPrompt) {
        this.newsletterPrompt = newsletterPrompt;
    }

    public String getLinkedInPrompt() {
        return linkedinPrompt;
    }

    public void setLinkedInPrompt(String linkedinPrompt) {
        this.linkedinPrompt = linkedinPrompt;
    }

    public String getTwitterPrompt() {
        return twitterPrompt;
    }

    public void setTwitterPrompt(String twitterPrompt) {
        this.twitterPrompt = twitterPrompt;
    }

    public Integer getMaxItemsPerChannel() {
        return maxItemsPerChannel;
    }

    public void setMaxItemsPerChannel(Integer maxItemsPerChannel) {
        this.maxItemsPerChannel = maxItemsPerChannel;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }
}
