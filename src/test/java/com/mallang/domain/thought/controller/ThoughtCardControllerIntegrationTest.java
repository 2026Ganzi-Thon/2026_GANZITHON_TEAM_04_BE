package com.mallang.domain.thought.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mallang.common.exception.ExceptionAdvice;
import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.concern.service.ConcernSessionService;
import com.mallang.domain.thought.service.ThoughtCardService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "UPSTAGE_API_KEY=")
@Tag("integration")
@Transactional
class ThoughtCardControllerIntegrationTest {
    @Autowired AnonymousUserService users;
    @Autowired ConcernSessionService sessions;
    @Autowired ThoughtCardService cards;

    @Test
    void onlySessionOwnerCanGenerateCardsWithoutReturningOriginalText() throws Exception {
        var owner = users.register();
        var other = users.register();
        String sessionId = sessions.start(owner.userId());
        sessions.select(owner.userId(), sessionId, SituationType.OTHER, 3);
        sessions.enterText(owner.userId(), sessionId, "발표가 걱정돼. 준비를 못했어.");
        var mvc = MockMvcBuilders.standaloneSetup(new ThoughtCardController(users, cards))
                .setControllerAdvice(new ExceptionAdvice()).build();

        mvc.perform(post("/api/sessions/{sessionId}/thought-cards", sessionId))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sessions/{sessionId}/thought-cards", sessionId)
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/sessions/{sessionId}/thought-cards", sessionId)
                        .header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generationMethod").value("fallback"))
                .andExpect(jsonPath("$.cards.length()").value(2))
                .andExpect(jsonPath("$.rawText").doesNotExist());
    }
}
