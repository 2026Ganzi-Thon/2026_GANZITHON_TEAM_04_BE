package com.mallang.domain.path.controller;

import com.mallang.common.response.ApiResponse;
import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.path.dto.request.PathRecommendRequest;
import com.mallang.domain.path.dto.response.PathRecommendResponse;
import com.mallang.domain.path.enums.PathType;
import com.mallang.domain.path.service.ActionRecommendationService;
import com.mallang.domain.path.service.ActionRecommendationService.Recommendations;
import com.mallang.domain.path.service.PathWorkflowService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "anonymousBearer")
@RequestMapping("/api/v1")
public class PathWorkflowController {
    private final AnonymousUserService users;
    private final PathWorkflowService paths;
    private final ActionRecommendationService actions;

    public PathWorkflowController(AnonymousUserService users, PathWorkflowService paths, ActionRecommendationService actions) {
        this.users = users;
        this.paths = paths;
        this.actions = actions;
    }

    @PostMapping("/concerns/{sessionId}/path/recommendation")
    public ApiResponse<PathRecommendResponse> recommend(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String sessionId, @Valid @RequestBody PathRecommendRequest request) {
        return ApiResponse.onSuccess(paths.recommend(users.requireUser(authorization), sessionId, request));
    }

    @PutMapping("/concerns/{sessionId}/path/selection")
    public ApiResponse<PathSelection> select(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @PathVariable String sessionId, @Valid @RequestBody PathSelection request) {
        return ApiResponse.onSuccess(new PathSelection(
                paths.select(users.requireUser(authorization), sessionId, request.selectedPath())));
    }

    @PostMapping("/sessions/{sessionId}/action-recommendations")
    public ApiResponse<Recommendations> actions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable String sessionId) {
        return ApiResponse.onSuccess(actions.recommend(users.requireUser(authorization), sessionId));
    }

    public record PathSelection(@NotNull PathType selectedPath) {}
}
