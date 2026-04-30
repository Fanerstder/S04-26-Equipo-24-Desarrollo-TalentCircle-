package com.talentcircle.domain.port.in;

import java.util.List;

public interface DraftReviewUseCase {
    record DraftSummaryDto(
            String id,
            String channel,
            String status,
            String createdAt,
            String summary
    ) {}

    record DraftDetailDto(
            String id,
            String channel,
            String content,
            String editedContent,
            String status,
            Double aiScore,
            String createdAt,
            String updatedAt,
            List<SourceDto> sources,
            List<DraftVersionDto> versions
    ) {}

    record SourceDto(String id, String title, Double relevanceScore) {}
    record DraftVersionDto(String id, String content, String editedBy, String editedAt, Integer versionNumber) {}

    record UpdateContentRequest(String content) {}
    record ApproveRequest() {}
    record RejectRequest(String reason) {}

    List<DraftSummaryDto> listDrafts(String channel, String status, String weekStart, String weekEnd, int page, int size);
    DraftDetailDto getDraftDetail(String draftId);
    DraftDetailDto updateContent(String draftId, UpdateContentRequest request);
    DraftDetailDto approveDraft(String draftId);
    DraftDetailDto rejectDraft(String draftId, RejectRequest request);
}
