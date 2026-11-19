package com.mallang.domain.thought.service;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.repository.ActiveSessionStore;
import com.mallang.domain.concern.repository.ActiveSessionStore.Session;
import com.mallang.domain.safety.service.SafetyService;
import com.mallang.domain.thought.dto.response.CardGenerateResponse;
import com.mallang.domain.thought.dto.response.CardResponse;
import com.mallang.domain.thought.enums.CardCategory;
import com.mallang.domain.thought.enums.GeneratedBy;
import com.mallang.domain.thought.exception.CardGenerationException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ThoughtCardService {
    private static final int MAX_CARDS = 8;
    private static final Pattern SEPARATOR = Pattern.compile("[\\p{P}\\p{Z}\\s]*");
    private static final String CARD_SELECT = "SELECT card_id, seq, content, category FROM thought_card WHERE session_id = ?";
    private final JdbcTemplate jdbc;
    private final ActiveSessionStore sessions;
    private final AiCardGenerator ai;
    private final RuleCardGenerator fallback;
    private final SafetyService safety;
    private final TransactionTemplate transaction;

    public ThoughtCardService(JdbcTemplate jdbc, ActiveSessionStore sessions, AiCardGenerator ai,
                              RuleCardGenerator fallback, SafetyService safety,
                              PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.sessions = sessions;
        this.ai = ai;
        this.fallback = fallback;
        this.safety = safety;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public CardGenerateResponse generate(String userId, String sessionId) {
        Session original = sessions.require(userId, sessionId, false);
        original.requireSafeInput();
        if (original.generatedBy() != null) {
            throw alreadyGenerated();
        }
        Generated generated = generateText(original.rawText());
        return transaction.execute(status -> {
            Session current = sessions.require(userId, sessionId, true);
            current.requireSafeInput();
            if (!Objects.equals(original.rawText(), current.rawText())) {
                throw new GeneralException(HttpStatus.CONFLICT, "고민이 변경됐습니다. 다시 카드를 만들어 주세요.");
            }
            if (current.generatedBy() != null) {
                throw alreadyGenerated();
            }
            List<String> texts = generated.texts();
            for (int i = 0; i < Math.min(texts.size(), MAX_CARDS); i++) {
                insert(current.id(), i + 1, texts.get(i));
            }
            boolean truncated = texts.size() > MAX_CARDS;
            jdbc.update("UPDATE active_session SET cards_generated_by = ? WHERE session_id = ?",
                    generated.by().name(), current.id());
            sessions.touch(current.id());
            return new CardGenerateResponse(cards(current.id()), generated.by(), truncated);
        });
    }

    @Transactional
    public List<CardResponse> list(String userId, String sessionId) {
        Session session = lockedSession(userId, sessionId);
        sessions.touch(session.id());
        return cards(session.id());
    }

    @Transactional
    public CardResponse add(String userId, String sessionId, String content) {
        requireContent(content);
        Session session = lockedSession(userId, sessionId);
        List<CardResponse> current = cards(session.id());
        if (current.size() >= MAX_CARDS) {
            throw new GeneralException(HttpStatus.CONFLICT, "CARD_LIMIT_EXCEEDED", "카드는 최대 8장까지 만들 수 있습니다.");
        }
        int seq = current.isEmpty() ? 1 : current.getLast().seq() + 1;
        insert(session.id(), seq, content);
        sessions.touch(session.id());
        return cards(session.id()).getLast();
    }

    @Transactional
    public EditedCard update(String userId, long cardId, String content) {
        requireContent(content);
        Session session = lockForCard(userId, cardId);
        CardResponse existing = requireCard(session.id(), cardId);
        sessions.touch(session.id());
        if (existing.content().equals(content)) {
            return new EditedCard(existing, false);
        }
        jdbc.update("UPDATE thought_card SET content = ?, category = 'UNCLASSIFIED' WHERE card_id = ?", content, cardId);
        boolean cleared = Objects.equals(session.coreCardId(), cardId);
        if (cleared) {
            sessions.clearPath(session.id());
        }
        return new EditedCard(new CardResponse(cardId, existing.seq(), content, CardCategory.UNCLASSIFIED), cleared);
    }

    @Transactional
    public void delete(String userId, long cardId) {
        Session session = lockForCard(userId, cardId);
        List<CardResponse> current = cards(session.id());
        CardResponse card = current.stream().filter(item -> item.cardId() == cardId).findFirst()
                .orElseThrow(ThoughtCardService::missingCard);
        if (current.size() == 1) {
            throw new GeneralException(HttpStatus.CONFLICT, "LAST_CARD", "카드는 한 장 이상 필요해요.");
        }
        if (Objects.equals(session.coreCardId(), cardId)) {
            sessions.clearPath(session.id());
        }
        jdbc.update("DELETE FROM thought_card WHERE card_id = ?", cardId);
        jdbc.update("UPDATE thought_card SET seq = seq - 1 WHERE session_id = ? AND seq > ? ORDER BY seq",
                session.id(), card.seq());
        sessions.touch(session.id());
    }

    @Transactional
    public List<CardResponse> classify(String userId, String sessionId, List<Classification> items) {
        Session session = lockedSession(userId, sessionId);
        var owned = new HashSet<Long>();
        cards(session.id()).forEach(card -> owned.add(card.cardId()));
        var changes = new HashMap<Long, CardCategory>();
        for (Classification item : items) {
            if (item == null || item.cardId() == null || item.category() == null
                    || changes.putIfAbsent(item.cardId(), item.category()) != null) {
                throw new GeneralException(HttpStatus.BAD_REQUEST, "카드 분류를 확인해 주세요.");
            }
            if (!owned.contains(item.cardId())) {
                throw missingCard();
            }
        }
        changes.forEach((cardId, category) -> jdbc.update("UPDATE thought_card SET category = ? WHERE card_id = ?",
                category.name(), cardId));
        sessions.touch(session.id());
        return cards(session.id());
    }

    public CardResponse requireCard(String internalSessionId, long cardId) {
        return jdbc.query(CARD_SELECT + " AND card_id = ? FOR UPDATE", (rs, row) -> mapCard(rs),
                        internalSessionId, cardId).stream().findFirst().orElseThrow(ThoughtCardService::missingCard);
    }

    private Session lockedSession(String userId, String id) {
        Session session = sessions.require(userId, id, true);
        session.requireSafeInput();
        if (session.generatedBy() == null) {
            throw new GeneralException(HttpStatus.CONFLICT, "카드를 먼저 생성해 주세요.");
        }
        return session;
    }

    private Session lockForCard(String userId, long cardId) {
        String sessionId = jdbc.query("SELECT session_id FROM thought_card WHERE card_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, cardId);
        if (sessionId == null) {
            throw missingCard();
        }
        return lockedSession(userId, sessionId);
    }

    private List<CardResponse> cards(String id) {
        return jdbc.query(CARD_SELECT + " ORDER BY seq FOR UPDATE", (rs, row) -> mapCard(rs), id);
    }

    private CardResponse mapCard(ResultSet rs) throws SQLException {
        return new CardResponse(rs.getLong(1), rs.getInt(2), rs.getString(3), CardCategory.valueOf(rs.getString(4)));
    }

    private void insert(String sessionId, int seq, String content) {
        jdbc.update("INSERT INTO thought_card (session_id, seq, content, category) VALUES (?, ?, ?, 'UNCLASSIFIED')",
                sessionId, seq, content);
    }

    private Generated generateText(String rawText) {
        try {
            List<String> result = ai.generate(rawText);
            if (preservesSource(rawText, result)) {
                return new Generated(result, GeneratedBy.AI);
            }
        } catch (CardGenerationException ignored) {
            // The provider may include private input in error details.
        }
        List<String> result = fallback.generate(rawText);
        return new Generated(result.isEmpty() ? List.of(rawText) : result, GeneratedBy.RULE);
    }

    private boolean preservesSource(String text, List<String> cards) {
        String source = text.replaceAll("(?U)\\s+", " ");
        int next = 0;
        for (String card : cards) {
            String normalized = card.replaceAll("(?U)\\s+", " ");
            int index = source.indexOf(normalized, next);
            if (index < 0 || !SEPARATOR.matcher(source.substring(next, index)).matches()) {
                return false;
            }
            next = index + normalized.length();
        }
        return !cards.isEmpty() && SEPARATOR.matcher(source.substring(next)).matches();
    }

    private void requireContent(String content) {
        if (content == null || content.codePoints().allMatch(c -> Character.isWhitespace(c)
                || Character.isSpaceChar(c) || c == 0x85)) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "EMPTY_CARD", "카드 내용을 입력해 주세요.");
        }
        if (safety.check(content).detected()) {
            throw new GeneralException(HttpStatus.CONFLICT, "SAFETY_GUIDANCE_REQUIRED", "상담 안내를 먼저 확인해 주세요.");
        }
    }

    private static GeneralException missingCard() {
        return new GeneralException(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다.");
    }

    private static GeneralException alreadyGenerated() {
        return new GeneralException(HttpStatus.CONFLICT, "CARDS_ALREADY_EXIST", "이미 만들어진 카드를 사용해 주세요.");
    }

    public record Classification(Long cardId, CardCategory category) {}
    public record EditedCard(CardResponse card, boolean coreSelectionCleared) {}
    private record Generated(List<String> texts, GeneratedBy by) {}
}
