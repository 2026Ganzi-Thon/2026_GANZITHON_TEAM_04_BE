package com.mallang.domain.reflection.service;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.concern.repository.ActiveSessionStore;
import com.mallang.domain.concern.repository.ActiveSessionStore.Session;
import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.path.enums.PathType;
import com.mallang.domain.reflection.dto.ReflectionSummary;
import com.mallang.domain.reminder.service.ReminderService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReflectionService {
    private static final String SELECT = """
            SELECT reflection_id, completed_at, situation_type, weight_before, recommended_route, chosen_route, weight_reduced
            FROM session_summary WHERE user_id = ?
            """;
    private final JdbcTemplate jdbc;
    private final ActiveSessionStore sessions;
    private final AnonymousUserService users;
    private final ReminderService reminders;

    public ReflectionService(JdbcTemplate jdbc, ActiveSessionStore sessions, AnonymousUserService users,
                             ReminderService reminders) {
        this.jdbc = jdbc;
        this.sessions = sessions;
        this.users = users;
        this.reminders = reminders;
    }

    @Transactional
    public Completion complete(String userId, String id, boolean moodReduced) {
        sessions.lockUser(userId);
        Optional<Session> active = sessions.find(userId, id, true);
        if (active.isEmpty()) {
            ReflectionSummary previous = findCompletedSession(userId, id).orElseThrow(ReflectionService::missing);
            if (previous.moodReduced() != moodReduced) {
                throw new GeneralException(HttpStatus.CONFLICT, "ALREADY_COMPLETED", "이미 완료된 세션입니다.");
            }
            return completion(previous, users.stepCount(userId));
        }
        Session session = active.get();
        if (session.expired()) {
            throw new GeneralException(HttpStatus.GONE, "SESSION_EXPIRED", "진행 세션이 만료됐습니다.");
        }
        session.requireSafeInput();
        if (session.recommended() == null || session.chosen() == null) {
            throw new GeneralException(HttpStatus.CONFLICT, "진행할 길을 먼저 선택해 주세요.");
        }
        jdbc.update("""
                INSERT INTO session_summary (session_id, active_public_id, user_id, completed_at, situation_type,
                    weight_before, recommended_route, chosen_route, weight_reduced)
                VALUES (?, ?, ?, UTC_TIMESTAMP(6), ?, ?, ?, ?, ?)
                """, session.id(), session.publicId(), userId, session.situation().name(), session.weight(),
                session.recommended().name(), session.chosen().name(), moodReduced);
        jdbc.update("UPDATE app_user SET step_count = step_count + 1 WHERE user_id = ?", userId);
        if (session.chosen() == PathType.DEFER) {
            reminders.createAfterCompletion(userId, session.id());
        }
        jdbc.update("DELETE FROM active_session WHERE session_id = ?", session.id());
        var summary = findCompletedSession(userId, session.id()).orElseThrow(ReflectionService::missing);
        return completion(summary, users.stepCount(userId));
    }

    public UserProgress progress(String userId) {
        int steps = users.stepCount(userId);
        return new UserProgress(steps, growthStage(steps));
    }

    @Transactional(readOnly = true)
    public ReflectionPage list(String userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "페이지 범위가 올바르지 않습니다.");
        }
        List<ReflectionSummary> items = jdbc.query(SELECT + " ORDER BY completed_at DESC, reflection_id DESC LIMIT ? OFFSET ?",
                (rs, row) -> map(rs), userId, size, (long) page * size);
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM session_summary WHERE user_id = ?", Long.class, userId);
        return new ReflectionPage(items, page, size, count);
    }

    public ReflectionSummary detail(String userId, long reflectionId) {
        return jdbc.query(SELECT + " AND reflection_id = ?", (rs, row) -> map(rs), userId, reflectionId)
                .stream().findFirst().orElseThrow(ReflectionService::missing);
    }

    @Transactional
    public void delete(String userId, long reflectionId) {
        String id = jdbc.query("SELECT session_id FROM session_summary WHERE reflection_id = ? AND user_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, reflectionId, userId);
        if (id == null) {
            throw missing();
        }
        reminders.deleteCompletedRecord(userId, id);
    }

    private Optional<ReflectionSummary> findCompletedSession(String userId, String id) {
        if (id.matches("[0-9]{1,19}")) {
            try {
                return jdbc.query(SELECT + " AND active_public_id = ?", (rs, row) -> map(rs), userId, Long.parseLong(id))
                        .stream().findFirst();
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }
        return jdbc.query(SELECT + " AND session_id = ?", (rs, row) -> map(rs), userId, id).stream().findFirst();
    }

    private ReflectionSummary map(ResultSet rs) throws SQLException {
        return new ReflectionSummary(rs.getLong(1),
                rs.getObject("completed_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                SituationType.valueOf(rs.getString(3)).apiValue(), rs.getInt(4),
                PathType.valueOf(rs.getString(5)), PathType.valueOf(rs.getString(6)), rs.getBoolean(7));
    }

    private Completion completion(ReflectionSummary summary, int steps) {
        return new Completion(summary, steps, growthStage(steps), !summary.moodReduced());
    }

    private String growthStage(int steps) {
        if (steps >= 5) return "구름 같은 털";
        if (steps >= 3) return "몽실 털";
        if (steps >= 1) return "풀린 털";
        return "엉킨 털";
    }

    private static GeneralException missing() {
        return new GeneralException(HttpStatus.NOT_FOUND, "완료 기록 또는 진행 세션을 찾을 수 없습니다.");
    }

    public record Completion(ReflectionSummary reflection, int stepCount, String growthStage, boolean showCounselingLink) {}
    public record UserProgress(int stepCount, String growthStage) {}
    public record ReflectionPage(List<ReflectionSummary> items, int page, int size, long totalElements) {}
}
