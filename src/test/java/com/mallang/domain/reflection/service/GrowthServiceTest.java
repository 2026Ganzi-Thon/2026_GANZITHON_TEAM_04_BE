package com.mallang.domain.reflection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mallang.domain.reflection.enums.GrowthStage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class GrowthServiceTest {

    private final GrowthService service = new GrowthService();

    @ParameterizedTest
    @CsvSource({
            "0,   TANGLED",
            "1,   UNTANGLED",
            "2,   UNTANGLED",
            "3,   FLUFFY",
            "4,   FLUFFY",
            "5,   CLOUD",
            "100, CLOUD"
    })
    void 걸음_수에_맞는_성장_단계를_돌려준다(int stepCount, GrowthStage expected) {
        assertThat(service.stageOf(stepCount)).isEqualTo(expected);
    }

    @Test
    void 단계는_명세의_이름을_가진다() {
        assertThat(GrowthStage.TANGLED.getLabel()).isEqualTo("엉킨 털");
        assertThat(GrowthStage.UNTANGLED.getLabel()).isEqualTo("풀린 털");
        assertThat(GrowthStage.FLUFFY.getLabel()).isEqualTo("몽실 털");
        assertThat(GrowthStage.CLOUD.getLabel()).isEqualTo("구름 같은 털");
    }

    @Test
    void 음수_걸음_수는_허용하지_않는다() {
        assertThatThrownBy(() -> service.stageOf(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}