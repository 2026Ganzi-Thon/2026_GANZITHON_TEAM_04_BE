package com.mallang.domain.concern.repository;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.path.enums.CanAct;
import com.mallang.domain.path.enums.PathType;
import com.mallang.domain.thought.enums.GeneratedBy;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActiveSessionStore {
    private static final String SELECT = """
            SELECT session_id, public_id, situation_type, weight_before, raw_text, safety_required,
                   core_card_id, can_act, needs_time, recommended_route, chosen_route,
                   cards_generated_by,
                   last_activity_at <= CURRENT_TIMESTAMP(6) - INTERVAL 24 HOUR AS expired
            FROM active_session WHERE user_id = ? AND
            """;
    private final JdbcTemplate jdbc;

    public ActiveSessionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Session require(String userId, String id, boolean lock) {
        Session session = find(userId, id, lock).orElseThrow(() ->
                new GeneralException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));
        if (session.expired()) {
            throw new GeneralException(HttpStatus.GONE, "SESSION_EXPIRED", "진행 세션이 만료됐습니다.");
        }
        return session;
    }

    public Optional<Session> find(String userId, String id, boolean lock) {
        String filter = "session_id = ?";
        Object key = id;
        if (id.matches("[0-9]{1,19}")) {
            try {
                key = Long.parseLong(id);
                filter = "public_id = ?";
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }
        String sql = SELECT + filter + (lock ? " FOR UPDATE" : "");
        return Optional.ofNullable(jdbc.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return new Session(rs.getString("session_id"), rs.getLong("public_id"),
                    enumValue(SituationType.class, rs.getString("situation_type")),
                    rs.getObject("weight_before", Integer.class), rs.getString("raw_text"),
                    rs.getBoolean("safety_required"), rs.getObject("core_card_id", Long.class),
                    enumValue(CanAct.class, rs.getString("can_act")), rs.getObject("needs_time", Boolean.class),
                    enumValue(PathType.class, rs.getString("recommended_route")),
                    enumValue(PathType.class, rs.getString("chosen_route")),
                    enumValue(GeneratedBy.class, rs.getString("cards_generated_by")),
                    rs.getBoolean("expired"));
        }, userId, key));
    }

    public void lockUser(String userId) {
        if (jdbc.query("SELECT user_id FROM app_user WHERE user_id = ? FOR UPDATE",
                rs -> !rs.next(), userId)) {
            throw new GeneralException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
    }

    public void touch(String sessionId) {
        jdbc.update("UPDATE active_session SET last_activity_at = CURRENT_TIMESTAMP(6) WHERE session_id = ?", sessionId);
    }

    public void clearPath(String sessionId) {
        jdbc.update("""
                UPDATE active_session SET core_card_id = NULL, can_act = NULL, needs_time = NULL,
                    recommended_route = NULL, chosen_route = NULL WHERE session_id = ?
                """, sessionId);
    }

    public int deleteExpired() {
        return jdbc.update("""
                DELETE FROM active_session WHERE last_activity_at <= CURRENT_TIMESTAMP(6) - INTERVAL 24 HOUR
                ORDER BY last_activity_at LIMIT 1000
                """);
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    public record Session(String id, long publicId, SituationType situation, Integer weight, String rawText,
                          boolean safetyRequired, Long coreCardId, CanAct canAct, Boolean needsTime,
                          PathType recommended, PathType chosen, GeneratedBy generatedBy, boolean expired) {
        public void requireSafeInput() {
            if (safetyRequired) {
                throw new GeneralException(HttpStatus.CONFLICT, "SAFETY_GUIDANCE_REQUIRED", "상담 안내를 먼저 확인해 주세요.");
            }
            if (rawText == null) {
                throw new GeneralException(HttpStatus.CONFLICT, "고민을 먼저 입력해 주세요.");
            }
        }

        @Override
        public String toString() {
            return "Session[id=" + id + "]";
        }
    }
}
