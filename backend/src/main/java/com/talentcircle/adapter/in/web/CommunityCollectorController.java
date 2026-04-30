package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.CommunityCollectorUseCase;
import com.talentcircle.domain.port.in.CommunityCollectorUseCase.CommunityActivityDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController`
@RequestMapping("/api/v1/admin/collector")
public class CommunityCollectorController {

    private final CommunityCollectorUseCase collectorUseCase;

    public CommunityCollectorController(CommunityCollectorUseCase collectorUseCase) {
        this.collectorUseCase = collectorUseCase;
    }

    @PostMapping("/collect")
    public ResponseEntity<Void> triggerCollection(@RequestParam String executionId,
                                                @RequestParam String sourceId) {
        collectorUseCase.collectActivity(executionId, sourceId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/activities")
    public ResponseEntity<List<CommunityActivityDto>> getActivities(@RequestParam String executionId) {
        return ResponseEntity.ok(collectorUseCase.getActivitiesByExecution(executionId));
    }
}
