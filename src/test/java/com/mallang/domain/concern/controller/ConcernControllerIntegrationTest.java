package com.mallang.domain.concern.controller;

import com.mallang.common.exception.ExceptionAdvice;
import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.concern.service.ConcernSessionService;
import com.mallang.domain.reminder.controller.ReminderController;
import com.mallang.domain.reminder.service.ReminderService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Tag("integration")
@Transactional
class ConcernControllerIntegrationTest {
    @Autowired AnonymousUserService users;
    @Autowired ConcernSessionService sessions;
    @Autowired ReminderService reminders;

    @Test void firstTapAndConcernFlowRequireOwnerAndShowEmptyNotice() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(
                new ConcernController(users, sessions), new ReminderController(users, reminders))
                .setControllerAdvice(new ExceptionAdvice()).build();
        var owner = users.register();
        String authorization = "Bearer " + owner.token();
        String sessionId = sessions.start(owner.userId());

        mvc.perform(put("/api/v1/sessions/{sessionId}/concern", sessionId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situationType\":\"OTHER\",\"weightBefore\":3}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/sessions/{sessionId}/concern/text", sessionId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"시험이 걱정돼\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guidanceRequired").value(false))
                .andExpect(jsonPath("$.rawText").doesNotExist());
        mvc.perform(get("/api/v1/reminders/status").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.showNotice").value(false));
        mvc.perform(get("/api/v1/reminders/status"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/sessions"))
                .andExpect(status().isUnauthorized());
    }
}
