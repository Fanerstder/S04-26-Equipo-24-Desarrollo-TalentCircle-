package com.talentcircle.application.service;

import com.talentcircle.domain.model.CommunityActivity;
import com.talentcircle.domain.model.WeeklyExecution;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.out.CommunityActivityRepository;
import com.talentcircle.domain.port.out.WeeklyExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommunityCollectorService implements CommunityCollectorUseCase {

    private final CommunityActivityRepository activityRepository;
    private final WeeklyExecutionRepository executionRepository;

    public CommunityCollectorService(CommunityActivityRepository activityRepository,
                                  WeeklyExecutionRepository executionRepository) {
        this.activityRepository = activityRepository;
        this.executionRepository = executionRepository;
    }

    @Override
    public void collectActivity(String executionId, String sourceId) {
        // Fetch execution or throw exception
        // In real implementation: fetch from DB
        // For now, simulate activity collection

        CommunityActivity activity = new CommunityActivity();
        // Set properties based on source
        activityRepository.save(activity);
    }

    @Override
    public List<CommunityActivityDto> getActivitiesByExecution(String executionId) {
        List<CommunityActivity> activities = activityRepository.findByExecutionId(executionId);
        return activities.stream()
                .map(this::mapToDto)
                .toList();
    }

    private CommunityActivityDto mapToDto(CommunityActivity activity) {
        return new CommunityActivityDto(
                activity.getId(),
                activity.getTitle(),
                activity.getContent(),
                activity.getType() != null ? activity.getType().name() : null,
                activity.getReactionCount(),
                activity.getResponseCount(),
                activity.getShareCount(),
                activity.getAuthor(),
                activity.getSourceUrl()
        );
    }
}
