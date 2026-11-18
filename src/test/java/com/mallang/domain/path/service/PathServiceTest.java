package com.mallang.domain.path.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mallang.domain.path.dto.request.PathRecommendRequest;
import com.mallang.domain.path.dto.response.PathRecommendResponse;
import com.mallang.domain.path.enums.CanAct;
import com.mallang.domain.path.enums.PathType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PathServiceTest {

    private final PathService service = new PathService();

    @ParameterizedTest
    @CsvSource({
            "HAS_ACTION, false, 3, ACTION",
            "HAS_ACTION, false, 4, SWITCH",
            "HAS_ACTION, true,  1, ACTION",
            "HAS_ACTION, true,  5, SWITCH",
            "NO_ACTION,  true,  1, DEFER",
            "NO_ACTION,  true,  5, DEFER",
            "NO_ACTION,  false, 3, DEFER",
            "NO_ACTION,  false, 4, SWITCH",
            "UNKNOWN,    true,  2, DEFER",
            "UNKNOWN,    true,  5, DEFER",
            "UNKNOWN,    false, 1, DEFER",
            "UNKNOWN,    false, 5, SWITCH"
    })
    void 명세_표대로_경로를_추천한다(CanAct canAct, boolean needsTime, int initialWeight, PathType expected) {
        assertThat(service.decide(canAct, needsTime, initialWeight)).isEqualTo(expected);
    }

    @Test
    void 추천_응답에는_세_경로가_순서대로_들어간다() {
        PathRecommendResponse response = service.recommend(
                new PathRecommendRequest(10L, CanAct.HAS_ACTION, true), 3);

        assertThat(response.recommendedPath()).isEqualTo(PathType.ACTION);
        assertThat(response.options())
                .containsExactly(PathType.ACTION, PathType.SWITCH, PathType.DEFER);
    }
}