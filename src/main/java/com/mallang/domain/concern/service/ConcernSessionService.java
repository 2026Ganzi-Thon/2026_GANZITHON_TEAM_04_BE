package com.mallang.domain.concern.service;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.dto.request.ConcernInputRequest;
import com.mallang.domain.concern.dto.response.ConcernInputResult;
import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.concern.repository.ActiveSessionStore;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConcernSessionService {
    private final JdbcTemplate jdbc;
    private final ConcernInputService inputService;
    private final ActiveSessionStore sessions;

    public ConcernSessionService(JdbcTemplate jdbc, ConcernInputService inputService, ActiveSessionStore sessions) {
        this.jdbc = jdbc;
        this.inputService = inputService;
        this.sessions = sessions;
    }

    @Transactional
    public String start(String userId) {
        sessions.lockUser(userId);
        jdbc.update("DELETE FROM active_session WHERE user_id = ?", userId);
        String sessionId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO active_session (session_id, user_id, started_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))",
                sessionId, userId);
        return sessionId;
    }

    @Transactional
    public long startForClient(String userId) {
        return sessions.require(userId, start(userId), false).publicId();
    }

    @Transactional
    public void select(String userId, String sessionId, SituationType situation, Integer weight) {
        if (situation == null || weight == null || weight < 1 || weight > 5) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "상황과 마음의 무게(1~5)를 선택해 주세요.");
        }
        var session = sessions.require(userId, sessionId, true);
        if (situation != session.situation() || !weight.equals(session.weight())) {
            jdbc.update("UPDATE active_session SET situation_type = ?, weight_before = ? WHERE session_id = ?",
                    situation.name(), weight, session.id());
            sessions.clearPath(session.id());
        }
        sessions.touch(session.id());
    }

    @Transactional
    public ConcernInputResult enterText(String userId, String sessionId, String text) {
        var session = sessions.require(userId, sessionId, true);
        var result = inputService.prepare(new ConcernInputRequest(session.situation(), session.weight(), text));
        boolean safetyRequired = result.safety().detected();
        if (!Objects.equals(session.rawText(), result.rawText()) || session.safetyRequired() != safetyRequired) {
            sessions.clearPath(session.id());
            jdbc.update("DELETE FROM thought_card WHERE session_id = ?", session.id());
            jdbc.update("""
                    UPDATE active_session SET raw_text = ?, safety_required = ?,
                        cards_generated_by = NULL WHERE session_id = ?
                    """, result.rawText(), safetyRequired, session.id());
        }
        sessions.touch(session.id());
        return result;
    }

    @Transactional
    public void cancel(String userId, String sessionId) {
        var session = sessions.find(userId, sessionId, true).orElseThrow(() ->
                new GeneralException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));
        jdbc.update("DELETE FROM active_session WHERE session_id = ?", session.id());
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void deleteExpiredSessions() {
        sessions.deleteExpired();
    }
}
