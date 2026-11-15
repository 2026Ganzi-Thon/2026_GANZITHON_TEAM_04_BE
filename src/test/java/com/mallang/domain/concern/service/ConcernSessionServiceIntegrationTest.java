package com.mallang.domain.concern.service;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.enums.SituationType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("integration")
@Transactional
class ConcernSessionServiceIntegrationTest {
    @Autowired AnonymousUserService users;
    @Autowired ConcernSessionService sessions;
    @Autowired JdbcTemplate jdbc;

    @Test void storesValidInputOnlyForOwnerAndDeletesOnCancel() {
        var owner = users.register();
        var other = users.register();
        String sessionId = sessions.start(owner.userId());
        assertThrows(GeneralException.class,
                () -> sessions.select(other.userId(), sessionId, SituationType.OTHER, 3));
        sessions.select(owner.userId(), sessionId, SituationType.STUDY_OR_WORK, 4);
        assertThrows(GeneralException.class,
                () -> sessions.enterText(other.userId(), sessionId, "고민"));
        assertEquals("고민", sessions.enterText(owner.userId(), sessionId, "고민").rawText());
        assertEquals("고민", jdbc.queryForObject("SELECT raw_text FROM active_session WHERE session_id = ?",
                String.class, sessionId));
        assertThrows(GeneralException.class, () -> sessions.cancel(other.userId(), sessionId));
        sessions.cancel(owner.userId(), sessionId);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM active_session WHERE session_id = ?",
                Integer.class, sessionId));
    }

    @Test void crisisInputClearsPreviousTextAndNeverHandsItToCards() {
        var owner = users.register();
        String sessionId = sessions.start(owner.userId());
        sessions.select(owner.userId(), sessionId, SituationType.OTHER, 3);
        sessions.enterText(owner.userId(), sessionId, "평범한 고민");
        var crisis = sessions.enterText(owner.userId(), sessionId, "죽고 싶어");
        assertTrue(crisis.safety().detected());
        assertNull(crisis.rawText());
        assertNull(jdbc.queryForObject("SELECT raw_text FROM active_session WHERE session_id = ?",
                String.class, sessionId));
    }

    @Test void storesTwoHundredVisibleEmojiAsText() {
        var owner = users.register();
        String sessionId = sessions.start(owner.userId());
        sessions.select(owner.userId(), sessionId, SituationType.RELATIONSHIP, 5);
        String text = "👨‍👩‍👧‍👦".repeat(200);
        assertEquals(text, sessions.enterText(owner.userId(), sessionId, text).rawText());
        assertEquals(text, jdbc.queryForObject("SELECT raw_text FROM active_session WHERE session_id = ?",
                String.class, sessionId));
        assertThrows(GeneralException.class,
                () -> sessions.enterText(owner.userId(), sessionId, text + "가"));
    }
}
