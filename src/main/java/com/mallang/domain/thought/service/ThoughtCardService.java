package com.mallang.domain.thought.service;

import com.mallang.common.exception.GeneralException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Generates temporary cards for an owned, active session; it stores no cards. */
@Service
public class ThoughtCardService {
    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    private final JdbcTemplate jdbc;
    private final AiCardGenerator upstage;
    private final RuleCardGenerator fallback;

    public ThoughtCardService(JdbcTemplate jdbc, AiCardGenerator upstage, RuleCardGenerator fallback) {
        this.jdbc = jdbc;
        this.upstage = upstage;
        this.fallback = fallback;
    }

    public GeneratedCards generate(String userId, String sessionId) {
        String rawText = jdbc.query("SELECT raw_text FROM active_session WHERE session_id = ? AND user_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, sessionId, userId);
        if (rawText == null) {
            throw new GeneralException(HttpStatus.NOT_FOUND, "카드를 만들 세션을 찾을 수 없습니다.");
        }
        if (rawText.isBlank() || GRAPHEME.matcher(rawText).results().limit(201).count() > 200) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "고민 입력을 다시 확인해 주세요.");
        }

        List<String> texts = null;
        String method = "fallback";
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                List<String> candidate = upstage.generate(rawText);
                if (validCards(candidate)) {
                    texts = candidate;
                    method = "ai";
                    break;
                }
            } catch (RuntimeException ignored) {
                // Provider details and the original text must not reach application logs.
            }
        }
        if (texts == null) {
            texts = fallback.generate(rawText);
            if (texts.isEmpty()) texts = List.of(rawText);
        }
        boolean truncated = texts.size() > 8;
        List<String> resultTexts = texts;
        List<Card> cards = java.util.stream.IntStream.range(0, Math.min(8, texts.size()))
                .mapToObj(i -> new Card(UUID.randomUUID().toString(), i + 1, resultTexts.get(i)))
                .toList();
        return new GeneratedCards(cards, method, truncated);
    }

    private static boolean validCards(List<String> cards) {
        if (cards == null || cards.isEmpty() || cards.stream().anyMatch(card -> card == null
                || card.isBlank() || GRAPHEME.matcher(card).results().limit(201).count() > 200)) {
            return false;
        }
        return cards.stream().distinct().count() == cards.size();
    }

    public record Card(String id, int order, String text) {}
    public record GeneratedCards(List<Card> cards, String generationMethod, boolean truncated) {}
}
