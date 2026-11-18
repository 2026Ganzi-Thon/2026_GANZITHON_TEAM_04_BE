package com.mallang.domain.thought.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuleCardGeneratorTest {

    private final RuleCardGenerator generator = new RuleCardGenerator();

    @Test
    void 줄바꿈으로_나눈다() {
        assertThat(generator.generate("답장이 3시간째 없어\n화났나 봐"))
                .containsExactly("답장이 3시간째 없어", "화났나 봐");
    }

    @Test
    void 문장_부호로_나눈다() {
        assertThat(generator.generate("답장이 없어. 화났나 봐? 걱정돼!"))
                .containsExactly("답장이 없어.", "화났나 봐?", "걱정돼!");
    }

    @Test
    void 빈_글은_카드를_만들지_않는다() {
        assertThat(generator.generate("   ")).isEmpty();
    }
}