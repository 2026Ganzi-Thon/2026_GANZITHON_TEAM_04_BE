package com.mallang.domain.reflection.controller;

import com.mallang.common.response.ApiResponse;
import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.reflection.dto.ReflectionSummary;
import com.mallang.domain.reflection.service.ReflectionService;
import com.mallang.domain.reflection.service.ReflectionService.Completion;
import com.mallang.domain.reflection.service.ReflectionService.ReflectionPage;
import com.mallang.domain.reflection.service.ReflectionService.UserProgress;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "anonymousBearer")
@RequestMapping("/api/v1")
public class ReflectionController {
    private final AnonymousUserService users;
    private final ReflectionService reflections;

    public ReflectionController(AnonymousUserService users, ReflectionService reflections) {
        this.users = users;
        this.reflections = reflections;
    }

    @PostMapping("/concerns/{sessionId}/completion")
    public ApiResponse<Completion> complete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable String sessionId, @Valid @RequestBody CompletionRequest request) {
        return ApiResponse.onSuccess(reflections.complete(users.requireUser(authorization), sessionId, request.moodReduced()));
    }

    @GetMapping("/me/summary")
    public ApiResponse<UserProgress> progress(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.onSuccess(reflections.progress(users.requireUser(authorization)));
    }

    @GetMapping("/reflections")
    public ApiResponse<ReflectionPage> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.onSuccess(reflections.list(users.requireUser(authorization), page, size));
    }

    @GetMapping("/reflections/{reflectionId}")
    public ApiResponse<ReflectionSummary> detail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable long reflectionId) {
        return ApiResponse.onSuccess(reflections.detail(users.requireUser(authorization), reflectionId));
    }

    @DeleteMapping("/reflections/{reflectionId}")
    public ApiResponse<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable long reflectionId) {
        reflections.delete(users.requireUser(authorization), reflectionId);
        return ApiResponse.onSuccess();
    }

    public record CompletionRequest(@NotNull Boolean moodReduced) {}
}
