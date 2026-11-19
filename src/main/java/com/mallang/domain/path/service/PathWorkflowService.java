package com.mallang.domain.path.service;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.repository.ActiveSessionStore;
import com.mallang.domain.path.dto.request.PathRecommendRequest;
import com.mallang.domain.path.dto.response.PathRecommendResponse;
import com.mallang.domain.path.enums.PathType;
import com.mallang.domain.thought.service.ThoughtCardService;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PathWorkflowService {
    private final JdbcTemplate jdbc;
    private final ActiveSessionStore sessions;
    private final ThoughtCardService cards;
    private final PathService paths;

    public PathWorkflowService(JdbcTemplate jdbc, ActiveSessionStore sessions, ThoughtCardService cards, PathService paths) {
        this.jdbc = jdbc;
        this.sessions = sessions;
        this.cards = cards;
        this.paths = paths;
    }

    @Transactional
    public PathRecommendResponse recommend(String userId, String sessionId, PathRecommendRequest request) {
        var session = sessions.require(userId, sessionId, true);
        session.requireSafeInput();
        cards.requireCard(session.id(), request.coreCardId());
        var result = paths.recommend(request, session.weight());
        if (!Objects.equals(session.coreCardId(), request.coreCardId()) || session.canAct() != request.canAct()
                || !Objects.equals(session.needsTime(), request.needsTime())) {
            jdbc.update("""
                    UPDATE active_session SET core_card_id = ?, can_act = ?, needs_time = ?,
                        recommended_route = ?, chosen_route = NULL WHERE session_id = ?
                    """, request.coreCardId(), request.canAct().name(), request.needsTime(),
                    result.recommendedPath().name(), session.id());
        }
        sessions.touch(session.id());
        return result;
    }

    @Transactional
    public PathType select(String userId, String sessionId, PathType path) {
        var session = sessions.require(userId, sessionId, true);
        session.requireSafeInput();
        if (session.recommended() == null || session.coreCardId() == null) {
            throw new GeneralException(HttpStatus.CONFLICT, "길을 추천받은 뒤 선택해 주세요.");
        }
        jdbc.update("UPDATE active_session SET chosen_route = ? WHERE session_id = ?", path.name(), session.id());
        sessions.touch(session.id());
        return path;
    }

    @Transactional
    public ActionInput actionInput(String userId, String sessionId) {
        var session = sessions.require(userId, sessionId, true);
        session.requireSafeInput();
        if (session.chosen() != PathType.ACTION || session.coreCardId() == null) {
            throw new GeneralException(HttpStatus.CONFLICT, "행동하기를 선택해 주세요.");
        }
        var card = cards.requireCard(session.id(), session.coreCardId());
        sessions.touch(session.id());
        return new ActionInput(card.cardId(), """
                상황: %s
                핵심 카드: %s
                분류: %s
                지금 할 수 있는 일: %s
                시간이 필요한지: %s
                초기 마음 무게: %s
                """.formatted(session.situation().apiValue(), card.content(), card.category(), session.canAct(),
                session.needsTime(), session.weight()));
    }

    public record ActionInput(long coreCardId, String prompt) {
        @Override public String toString() { return "ActionInput[coreCardId=" + coreCardId + "]"; }
    }
}
