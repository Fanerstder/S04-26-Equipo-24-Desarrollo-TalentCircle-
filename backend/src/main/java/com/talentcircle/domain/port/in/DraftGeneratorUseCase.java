package com.talentcircle.domain.port.in;

import com.talentcircle.domain.model.Draft;
import java.util.List;

public interface DraftGeneratorUseCase {
    List<Draft> generateDrafts(String executionId);

    record DraftDto(
            String id,
            String channel,
            String content,
            String status,
            Double aiScore
    ) {}
}
