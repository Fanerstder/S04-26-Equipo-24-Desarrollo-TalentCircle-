package com.talentcircle.domain.port.in;

import java.util.List;

public interface AdminUseCase {
    record SourceDto(String id, String name, String type, boolean active) {}
    record CreateSourceRequest(String name, String type, String apiUrl, String apiKey) {}
    record UpdateSourceRequest(String name, String apiUrl, String apiKey, Boolean active) {}

    record ConfigDto(String llmProvider, String llmModel, String newsletterPrompt,
                       String linkedinPrompt, String twitterPrompt,
                       Integer maxItemsPerChannel, String scheduleCron) {}
    record UpdateConfigRequest(String llmProvider, String llmModel, String newsletterPrompt,
                               String linkedinPrompt, String twitterPrompt,
                               Integer maxItemsPerChannel, String scheduleCron) {}

    record UserDto(String id, String email, String fullName, String role, Boolean active) {}
    record CreateUserRequest(String email, String password, String fullName, String role) {}
    record UpdateUserRequest(String fullName, String role, Boolean active) {}

    record ExecutionSummaryDto(String id, String weekStart, String weekEnd,
                                 String status, String startedAt, String completedAt) {}

    List<SourceDto> getSources();
    SourceDto createSource(CreateSourceRequest request);
    SourceDto updateSource(String id, UpdateSourceRequest request);
    void deleteSource(String id);

    ConfigDto getConfig();
    ConfigDto updateConfig(UpdateConfigRequest request);

    List<UserDto> getUsers();
    UserDto createUser(CreateUserRequest request);
    UserDto updateUser(String id, UpdateUserRequest request);

    List<ExecutionSummaryDto> getExecutions();
    ExecutionSummaryDto getExecutionDetail(String id);
    void triggerExecution(String triggeredBy);
}
