package com.mallang.domain.concern.controller;

import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.concern.service.ConcernSessionService;
import jakarta.servlet.http.HttpServletResponse;
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

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionId start(@RequestHeader("Authorization") String authorization) {
        return new SessionId(sessions.start(users.requireUser(authorization)));
    }

    @PutMapping("/sessions/{sessionId}/concern")
    public void select(@RequestHeader("Authorization") String authorization,
                       @PathVariable String sessionId, @RequestBody Selection body) {
        sessions.select(users.requireUser(authorization), sessionId, body.situationType(), body.weightBefore());
    }

    @PutMapping("/sessions/{sessionId}/concern/text")
    public TextAccepted enterText(@RequestHeader("Authorization") String authorization,
                                  @PathVariable String sessionId, @RequestBody TextInput body) {
        var result = sessions.enterText(users.requireUser(authorization), sessionId, body.rawText());
        return new TextAccepted(result.safety().detected(), result.safety().guideType());
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@RequestHeader("Authorization") String authorization, @PathVariable String sessionId) {
        sessions.cancel(users.requireUser(authorization), sessionId);
    }

    public record SessionId(String sessionId) {}
    public record Selection(SituationType situationType, Integer weightBefore) {}
    public record TextInput(String rawText) {
        @Override public String toString() { return "TextInput[rawText=***]"; }
    }
    public record TextAccepted(boolean guidanceRequired, Object guideType) {}
}
