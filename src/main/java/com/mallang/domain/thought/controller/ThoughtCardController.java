package com.mallang.domain.thought.controller;

import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.thought.service.ThoughtCardService;
import com.mallang.domain.thought.service.ThoughtCardService.GeneratedCards;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/thought-cards")
public class ThoughtCardController {
    private final AnonymousUserService users;
    private final ThoughtCardService cards;

    public ThoughtCardController(AnonymousUserService users, ThoughtCardService cards) {
        this.users = users;
        this.cards = cards;
    }

    @PostMapping
    public GeneratedCards generate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String sessionId, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return cards.generate(users.requireUser(authorization), sessionId);
    }
}
