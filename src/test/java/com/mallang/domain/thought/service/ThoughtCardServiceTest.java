package com.mallang.domain.thought.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mallang.common.exception.GeneralException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

class ThoughtCardServiceTest {
    private JdbcTemplate jdbc;
    private UpstageThoughtClient upstage;
    private ThoughtCardService service;

    @BeforeEach
    void setup() {
        jdbc = mock(JdbcTemplate.class);
        upstage = mock(UpstageThoughtClient.class);
        service = new ThoughtCardService(jdbc, upstage, new RuleCardGenerator());
    }

    private void sessionText(String text) {
        when(jdbc.query(any(String.class), any(ResultSetExtractor.class),
                eq("session"), eq("owner"))).thenReturn(text);
    }

    @Test
    void nineAiCardsReturnFirstEightAndTruncationNotice() {
        sessionText("많은 생각이 있어.");
        when(upstage.split("많은 생각이 있어."))
                .thenReturn(IntStream.rangeClosed(1, 9).mapToObj(i -> "생각 " + i).toList());

        var result = service.generate("owner", "session");
        assertThat(result.generationMethod()).isEqualTo("ai");
        assertThat(result.truncated()).isTrue();
        assertThat(result.cards()).hasSize(8);
        assertThat(result.cards().get(0).text()).isEqualTo("생각 1");
        assertThat(result.cards().get(7).order()).isEqualTo(8);
        assertThat(result.cards().stream().map(ThoughtCardService.Card::id)).doesNotHaveDuplicates();
    }

    @Test
    void providerFailureUsesOriginalOrderAndFallback() {
        sessionText("첫째. 둘째. 셋째.");
        when(upstage.split("첫째. 둘째. 셋째."))
                .thenThrow(new IllegalStateException("Thought card generation failed"))
                .thenThrow(new IllegalStateException("Thought card generation failed"));

        var result = service.generate("owner", "session");
        assertThat(result.generationMethod()).isEqualTo("fallback");
        assertThat(result.truncated()).isFalse();
        assertThat(result.cards().stream().map(ThoughtCardService.Card::text))
                .containsExactly("첫째.", "둘째.", "셋째.");
        verify(upstage, times(2)).split("첫째. 둘째. 셋째.");
    }

    @Test
    void fallbackAlsoLimitsNineCards() {
        String rawText = "하나. 둘. 셋. 넷. 다섯. 여섯. 일곱. 여덟. 아홉.";
        sessionText(rawText);
        when(upstage.split(rawText)).thenThrow(new IllegalStateException());

        var result = service.generate("owner", "session");
        assertThat(result.generationMethod()).isEqualTo("fallback");
        assertThat(result.truncated()).isTrue();
        assertThat(result.cards()).hasSize(8);
        assertThat(result.cards().get(7).text()).isEqualTo("여덟.");
    }

    @Test
    void otherUsersSessionIsUnavailable() {
        assertThatThrownBy(() -> service.generate("other", "session"))
                .isInstanceOf(GeneralException.class);
    }
}
