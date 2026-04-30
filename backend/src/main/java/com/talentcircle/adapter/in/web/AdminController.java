package com.talentcircle.adapter.in.web;

import com.talentcircle.domain.port.in.AdminUseCase;
import com.talentcircle.domain.port.in.AdminUseCase.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminUseCase adminUseCase;

    public AdminController(AdminUseCase adminUseCase) {
        this.adminUseCase = adminUseCase;
    }

    // Sources endpoints
    @GetMapping("/sources")
    public ResponseEntity<List<SourceDto>> getSources() {
        return ResponseEntity.ok(adminUseCase.getSources());
    }

    @PostMapping("/sources")
    public ResponseEntity<SourceDto> createSource(@RequestBody CreateSourceRequest request) {
        return ResponseEntity.ok(adminUseCase.createSource(request));
    }

    @PutMapping("/sources/{id}")
    public ResponseEntity<SourceDto> updateSource(@PathVariable String id,
                                                  @RequestBody UpdateSourceRequest request) {
        return ResponseEntity.ok(adminUseCase.updateSource(id, request));
    }

    @DeleteMapping("/sources/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable String id) {
        adminUseCase.deleteSource(id);
        return ResponseEntity.noContent().build();
    }

    // Config endpoints
    @GetMapping("/config")
    public ResponseEntity<ConfigDto> getConfig() {
        return ResponseEntity.ok(adminUseCase.getConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<ConfigDto> updateConfig(@RequestBody UpdateConfigRequest request) {
        return ResponseEntity.ok(adminUseCase.updateConfig(request));
    }

    // Users endpoints
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(adminUseCase.getUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(adminUseCase.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id,
                                              @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(adminUseCase.updateUser(id, request));
    }

    // Executions endpoints
    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionSummaryDto>> getExecutions() {
        return ResponseEntity.ok(adminUseCase.getExecutions());
    }

    @GetMapping("/executions/{id}")
    public ResponseEntity<ExecutionSummaryDto> getExecutionDetail(@PathVariable String id) {
        return ResponseEntity.ok(adminUseCase.getExecutionDetail(id));
    }

    @PostMapping("/executions/trigger")
    public ResponseEntity<Void> triggerExecution(@RequestParam String triggeredBy) {
        adminUseCase.triggerExecution(triggeredBy);
        return ResponseEntity.accepted().build();
    }
}
