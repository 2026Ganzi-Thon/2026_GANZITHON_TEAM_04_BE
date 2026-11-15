package com.mallang.domain.reminder.service;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.service.AnonymousUserService;
import java.sql.Timestamp;
import java.time.Instant;
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
class ReminderServiceIntegrationTest {
    @Autowired AnonymousUserService users;
    @Autowired ReminderService reminders;
    @Autowired JdbcTemplate jdbc;

    @Test void limitsEachOwnerToFiveAndDoesNotDeleteSummariesOrSteps() {
        var owner = users.register();
        var other = users.register();
        assertEquals(owner.userId(), users.requireUser("Bearer " + owner.token()));
        assertThrows(GeneralException.class, () -> users.requireUser("Bearer invalid"));
        assertFalse(reminders.status(owner.userId()).showNotice());

        for (int n = 0; n < 6; n++) {
            String sessionId = java.util.UUID.randomUUID().toString();
            jdbc.update("INSERT INTO session_summary (session_id, user_id, completed_at, "
                    + "situation_type, weight_before, recommended_route, chosen_route, weight_reduced) "
                    + "VALUES (?, ?, ?, 'OTHER', 3, 'DEFER', 'DEFER', true)",
                    sessionId, owner.userId(), Timestamp.from(Instant.now()));
            reminders.createAfterCompletion(owner.userId(), sessionId);
        }
        assertEquals(5, reminders.status(owner.userId()).count());
        assertEquals(5, reminders.list(owner.userId()).size());
        assertEquals(0, reminders.status(other.userId()).count());
        Long summaries = jdbc.queryForObject("SELECT COUNT(*) FROM session_summary WHERE user_id = ?",
                Long.class, owner.userId());
        assertEquals(6L, summaries);
        long reminderId = reminders.list(owner.userId()).getFirst().reminderId();
        assertThrows(GeneralException.class, () -> reminders.detail(other.userId(), reminderId));
        assertThrows(GeneralException.class, () -> reminders.delete(other.userId(), reminderId));
        reminders.delete(owner.userId(), reminderId);
        assertEquals(4, reminders.status(owner.userId()).count());
        assertEquals(6L, jdbc.queryForObject("SELECT COUNT(*) FROM session_summary WHERE user_id = ?",
                Long.class, owner.userId()));
        assertEquals(0, jdbc.queryForObject("SELECT step_count FROM app_user WHERE user_id = ?",
                Integer.class, owner.userId()));
    }

    @Test void completedRecordDeletionCascadesReminderAndDeductsOneStepOnce() {
        var owner = users.register();
        var other = users.register();
        String sessionId = java.util.UUID.randomUUID().toString();
        jdbc.update("UPDATE app_user SET step_count = 1 WHERE user_id = ?", owner.userId());
        jdbc.update("INSERT INTO session_summary (session_id, user_id, completed_at, "
                + "situation_type, weight_before, recommended_route, chosen_route, weight_reduced) "
                + "VALUES (?, ?, ?, 'OTHER', 3, 'DEFER', 'DEFER', true)",
                sessionId, owner.userId(), Timestamp.from(Instant.now()));
        reminders.createAfterCompletion(owner.userId(), sessionId);
        assertThrows(GeneralException.class,
                () -> reminders.deleteCompletedRecord(other.userId(), sessionId));
        reminders.deleteCompletedRecord(owner.userId(), sessionId);
        assertEquals(0, reminders.status(owner.userId()).count());
        assertEquals(0, jdbc.queryForObject("SELECT step_count FROM app_user WHERE user_id = ?",
                Integer.class, owner.userId()));
        assertThrows(GeneralException.class,
                () -> reminders.deleteCompletedRecord(owner.userId(), sessionId));
        assertEquals(0, jdbc.queryForObject("SELECT step_count FROM app_user WHERE user_id = ?",
                Integer.class, owner.userId()));
    }
}
