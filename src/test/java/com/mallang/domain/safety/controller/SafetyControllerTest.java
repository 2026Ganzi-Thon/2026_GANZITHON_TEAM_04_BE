package com.mallang.domain.safety.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.mallang.common.response.ApiResponse;
import com.mallang.domain.safety.dto.request.SafetyCheckRequest;
import com.mallang.domain.safety.dto.response.SafetyCheckResponse;
import com.mallang.domain.safety.dto.response.SafetyGuideListResponse;
import com.mallang.domain.safety.enums.GuideType;
import com.mallang.domain.safety.service.SafetyService;
import org.junit.jupiter.api.Test;

class SafetyControllerTest {

    private final SafetyController controller = new SafetyController(new SafetyService());

    @Test
    void 위기_표현이면_CRISIS로_응답한다() {
        ApiResponse<SafetyCheckResponse> response = controller.check(new SafetyCheckRequest("죽고 싶어"));

        assertThat(response.success()).isTrue();
        assertThat(response.data().detected()).isTrue();
        assertThat(response.data().guideType()).isEqualTo(GuideType.CRISIS);
    }

    @Test
    void 일상_고민은_감지하지_않는다() {
        ApiResponse<SafetyCheckResponse> response = controller.check(new SafetyCheckRequest("답장이 없어"));

        assertThat(response.data().detected()).isFalse();
        assertThat(response.data().guideType()).isNull();
    }

    @Test
    void 안내_목록을_guides로_감싸서_돌려준다() {
        ApiResponse<SafetyGuideListResponse> response = controller.guides();

        assertThat(response.success()).isTrue();
        assertThat(response.data().guides()).hasSize(2);
    }
}