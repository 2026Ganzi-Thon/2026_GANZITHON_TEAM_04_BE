package com.mallang.domain.concern.service;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.dto.request.ConcernInputRequest;
import com.mallang.domain.concern.dto.response.ConcernInputResult;
import com.mallang.domain.concern.enums.SituationType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConcernSessionService {
    private final JdbcTemplate jdbc;
    private final ConcernInputService inputService;

    public ConcernSessionService(JdbcTemplate jdbc, ConcernInputService inputService) {
        this.jdbc = jdbc;
        this.inputService = inputService;
    }

    @Transactional
    public String start(String userId) {
        String sessionId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO active_session (session_id, user_id, started_at) VALUES (?, ?, ?)",
                sessionId, userId, Timestamp.from(Instant.now()));
        return sessionId;
    }

    @Transactional
    public void select(String userId, String sessionId, SituationType situation, Integer weight) {
        if (situation == null || weight == null || weight < 1 || weight > 5) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "상황과 마음의 무게(1~5)를 선택해 주세요.");
        }
        int changed = jdbc.update("UPDATE active_session SET situation_type = ?, weight_before = ? "
                + "WHERE session_id = ? AND user_id = ?", situation.name(), weight, sessionId, userId);
        if (changed == 0) missing();
    }

    @Transactional
    public ConcernInputResult enterText(String userId, String sessionId, String text) {
        ConcernInputRequest request = jdbc.query("SELECT situation_type, weight_before "
                + "FROM active_session WHERE session_id = ? AND user_id = ? FOR UPDATE", rs -> {
            if (!rs.next()) return null;
            String situation = rs.getString(1);
            Integer weight = (Integer) rs.getObject(2);
            return new ConcernInputRequest(situation == null ? null : SituationType.valueOf(situation), weight, text);
        }, sessionId, userId);
        if (request == null) missing();
        ConcernInputResult result = inputService.prepare(request);
        if (result.safety().detected()) {
            jdbc.update("UPDATE active_session SET raw_text = NULL WHERE session_id = ? AND user_id = ?",
                    sessionId, userId);
            return result;
        }
        jdbc.update("UPDATE active_session SET raw_text = ? WHERE session_id = ? AND user_id = ?",
                result.rawText(), sessionId, userId);
        return result;
    }

    @Transactional
    public void cancel(String userId, String sessionId) {
        int deleted = jdbc.update("DELETE FROM active_session WHERE session_id = ? AND user_id = ?",
                sessionId, userId);
        if (deleted == 0) missing();
    }

    private void missing() {
        throw new GeneralException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다.");
    }
}
