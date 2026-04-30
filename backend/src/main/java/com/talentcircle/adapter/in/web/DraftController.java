package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.DraftReviewUseCase;
import com.talentcircle.domain.port.in.DraftReviewUseCase.ApproveRequest;
import com.talentcircle.domain.port.in.DraftReviewUseCase.DraftDetailDto;
import com.talentcircle.domain.port.in.DraftReviewUseCase.DraftSummaryDto;
import com.talentcircle.domain.port.in.DraftReviewUseCase.RejectRequest;
import com.talentcircle.domain.port.in.DraftReviewUseCase.UpdateContentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drafts")
public class DraftController {

    private final DraftReviewUseCase draftReviewUseCase;

    public DraftController(DraftReviewUseCase draftReviewUseCase) {
        this.draftReviewUseCase = draftReviewUseCase;
    }

    @GetMapping
    public ResponseEntity<List<DraftSummaryDto>> listDrafts(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String weekStart,
            @RequestParam(required = false) String weekEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(draftReviewUseCase.listDrafts(channel, status, weekStart, weekEnd, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DraftDetailDto> getDraftDetail(@PathVariable String id) {
        return ResponseEntity.ok(draftReviewUseCase.getDraftDetail(id));
    }

    @PatchMapping("/{id}/content")
    public ResponseEntity<DraftDetailDto> updateContent(@PathVariable String id,
                                                 @RequestBody UpdateContentRequest request) {
        return ResponseEntity.ok(draftReviewUseCase.updateContent(id, request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<DraftDetailDto> approveDraft(@PathVariable String id,
                                                 @RequestBody(required = false) ApproveRequest request) {
        return ResponseEntity.ok(draftReviewUseCase.approveDraft(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<DraftDetailDto> rejectDraft(@PathVariable String id,
                                                 @RequestBody RejectRequest request) {
        return ResponseEntity.ok(draftReviewUseCase.rejectDraft(id, request));
    }
}
