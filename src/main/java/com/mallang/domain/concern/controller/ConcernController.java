package com.mallang.domain.concern.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.mallang.common.exception.GeneralException;
import com.mallang.common.response.ApiResponse;
import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.concern.service.ConcernSessionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ConcernController {
    private final AnonymousUserService users;
    private final ConcernSessionService sessions;

    public ConcernController(AnonymousUserService users, ConcernSessionService sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    /** Call on first-screen tap; store the token in device-local secure storage. */
    @PostMapping("/anonymous-users")
    @ResponseStatus(HttpStatus.CREATED)
    public AnonymousUserService.Registration register(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return users.register();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AnonymousUserService.Registration> createUser(HttpServletResponse response) {
        return ApiResponse.onSuccess(register(response));
    }

    @GetMapping("/users/{userId}")
    @SecurityRequirement(name = "anonymousBearer")
    public ApiResponse<AnonymousUserService.UserSummary> user(
            @RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable String userId) {
        return ApiResponse.onSuccess(users.summary(users.requireUser(authorization), userId));
    }

    @PostMapping("/sessions")
    @SecurityRequirement(name = "anonymousBearer")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionStarted> start(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.onSuccess(new SessionStarted(sessions.startForClient(users.requireUser(authorization)), "IN_PROGRESS"));
    }

    @PutMapping("/sessions/{sessionId}/concern")
    @SecurityRequirement(name = "anonymousBearer")
    public ApiResponse<SelectionSaved> select(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @PathVariable String sessionId, @RequestBody Selection body) {
        String userId = users.requireUser(authorization);
        SituationType situation = SituationType.fromApi(body.situationType());
        BigDecimal weight = body.initialWeight();
        if (weight == null || weight.compareTo(BigDecimal.ONE) < 0 || weight.compareTo(BigDecimal.valueOf(5)) > 0
                || weight.stripTrailingZeros().scale() > 0) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "마음의 무게(1~5)를 선택해 주세요.");
        }
        int initialWeight = weight.intValueExact();
        sessions.select(userId, sessionId, situation, initialWeight);
        return ApiResponse.onSuccess(new SelectionSaved(situation.apiValue(), initialWeight));
    }

    @PutMapping("/sessions/{sessionId}/concern/text")
    @SecurityRequirement(name = "anonymousBearer")
    public ApiResponse<TextAccepted> enterText(@RequestHeader(value = "Authorization", required = false) String authorization,
                                               @PathVariable String sessionId, @RequestBody TextInput body) {
        var result = sessions.enterText(users.requireUser(authorization), sessionId, body.content());
        if (result.safety().detected()) {
            throw new GeneralException(HttpStatus.CONFLICT, "SAFETY_GUIDANCE_REQUIRED", "상담 안내를 먼저 확인해 주세요.");
        }
        return ApiResponse.onSuccess(new TextAccepted(true));
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    @SecurityRequirement(name = "anonymousBearer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@RequestHeader(value = "Authorization", required = false) String authorization,
                       @PathVariable String sessionId) {
        sessions.cancel(users.requireUser(authorization), sessionId);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @SecurityRequirement(name = "anonymousBearer")
    public ApiResponse<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String sessionId) {
        cancel(authorization, sessionId);
        return ApiResponse.onSuccess();
    }

    public record SessionStarted(long sessionId, String status) {}
    public record Selection(String situationType, @JsonAlias("weightBefore") BigDecimal initialWeight) {}
    public record SelectionSaved(String situationType, int initialWeight) {}
    public record TextInput(@JsonAlias("rawText") String content) {
        @Override public String toString() { return "TextInput[content=***]"; }
    }
    public record TextAccepted(boolean accepted) {}
}
