package com.mallang.domain.reminder.service;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.path.enums.PathType;
import com.mallang.domain.reminder.dto.response.ReminderStatus;
import com.mallang.domain.reminder.dto.response.ReminderSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {
    private static final String SELECT = "SELECT d.deferred_record_id, s.session_id, s.completed_at, "
            + "s.situation_type, s.weight_before, s.recommended_route, s.chosen_route, s.weight_reduced "
            + "FROM deferred_record d JOIN session_summary s ON s.session_id = d.session_id ";
    private final JdbcTemplate jdbc;

    public ReminderService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ReminderStatus status(String userId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM deferred_record d "
                + "JOIN session_summary s ON s.session_id = d.session_id WHERE s.user_id = ?",
                Long.class, userId);
        return ReminderPolicy.status(count == null ? 0 : count);
    }

    @Transactional(readOnly = true)
    public List<ReminderSummary> list(String userId) {
        return jdbc.query(SELECT + "WHERE s.user_id = ? "
                + "ORDER BY d.created_at DESC, d.deferred_record_id DESC", this::map, userId);
    }

    @Transactional(readOnly = true)
    public ReminderSummary detail(String userId, long reminderId) {
        List<ReminderSummary> rows = jdbc.query(SELECT
                + "WHERE s.user_id = ? AND d.deferred_record_id = ?", this::map, userId, reminderId);
        if (rows.isEmpty()) {
            throw new GeneralException(HttpStatus.NOT_FOUND, "미루기 기록을 찾을 수 없습니다.");
        }
        return rows.getFirst();
    }

    /** Deletes only the deferred item; completed summary and steps remain intact. */
    @Transactional
    public void delete(String userId, long reminderId) {
        int deleted = jdbc.update("DELETE d FROM deferred_record d "
                + "JOIN session_summary s ON s.session_id = d.session_id "
                + "WHERE s.user_id = ? AND d.deferred_record_id = ?", userId, reminderId);
        if (deleted == 0) {
            throw new GeneralException(HttpStatus.NOT_FOUND, "미루기 기록을 찾을 수 없습니다.");
        }
    }

    /** Call inside the same transaction as completion, after the DEFER summary is inserted. */
    @Transactional
    public void createAfterCompletion(String userId, String sessionId) {
        List<String> locked = jdbc.query("SELECT user_id FROM app_user WHERE user_id = ? FOR UPDATE",
                (rs, row) -> rs.getString(1), userId);
        if (locked.isEmpty()) {
            throw new GeneralException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        List<String> routes = jdbc.query("SELECT chosen_route FROM session_summary "
                + "WHERE session_id = ? AND user_id = ?", (rs, row) -> rs.getString(1), sessionId, userId);
        if (routes.size() != 1 || !"DEFER".equals(routes.getFirst())) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "완료된 판단 미루기 세션이 필요합니다.");
        }
        jdbc.update("INSERT INTO deferred_record (session_id, created_at) VALUES (?, CURRENT_TIMESTAMP(6))",
                sessionId);
        List<ReminderPolicy.Entry> entries = jdbc.query("SELECT d.deferred_record_id, d.created_at "
                + "FROM deferred_record d JOIN session_summary s ON s.session_id = d.session_id "
                + "WHERE s.user_id = ?", (rs, row) -> new ReminderPolicy.Entry(
                        rs.getLong(1), rs.getTimestamp(2).toInstant()), userId);
        for (long id : ReminderPolicy.idsToEvict(entries)) {
            jdbc.update("DELETE FROM deferred_record WHERE deferred_record_id = ?", id);
        }
    }

    /** My-page deletion: the FK cascades to the reminder and one step is deducted once. */
    @Transactional
    public void deleteCompletedRecord(String userId, String sessionId) {
        List<Integer> steps = jdbc.query("SELECT step_count FROM app_user WHERE user_id = ? FOR UPDATE",
                (rs, row) -> rs.getInt(1), userId);
        if (steps.isEmpty()) {
            throw new GeneralException(HttpStatus.NOT_FOUND, "완료 기록을 찾을 수 없습니다.");
        }
        int deleted = jdbc.update("DELETE FROM session_summary WHERE session_id = ? AND user_id = ?",
                sessionId, userId);
        if (deleted == 0) {
            throw new GeneralException(HttpStatus.NOT_FOUND, "완료 기록을 찾을 수 없습니다.");
        }
        if (steps.getFirst() < 1) {
            throw new GeneralException(HttpStatus.CONFLICT, "걸음 수가 올바르지 않습니다.");
        }
        jdbc.update("UPDATE app_user SET step_count = step_count - 1 WHERE user_id = ?", userId);
    }

    private ReminderSummary map(ResultSet rs, int row) throws SQLException {
        return new ReminderSummary(rs.getLong(1), rs.getString(2), rs.getTimestamp(3).toInstant(),
                SituationType.valueOf(rs.getString(4)), rs.getInt(5),
                PathType.valueOf(rs.getString(6)), PathType.valueOf(rs.getString(7)), rs.getBoolean(8));
    }
}
