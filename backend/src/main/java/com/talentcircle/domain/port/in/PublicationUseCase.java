package com.talentcircle.domain.port.in;

public interface PublicationUseCase {
    record ExportRequest(String week, String format) {}

    PublicationDto publishDraft(String draftId);
    byte[] exportDrafts(ExportRequest request);

    record PublicationDto(
            String id,
            String draftId,
            String status,
            String externalPostId,
            String publishedAt,
            String errorMessage
    ) {}
}
