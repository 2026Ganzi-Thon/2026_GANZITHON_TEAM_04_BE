package com.mallang.domain.safety.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.mallang.domain.safety.dto.response.SafetyGuideResponse;
import com.mallang.domain.safety.enums.GuideType;
import org.junit.jupiter.api.Test;

class SafetyServiceTest {

    private final SafetyService service = new SafetyService();

    @Test
    void 위기_표현이_있으면_감지한다() {
        assertThat(service.check("요즘 너무 힘들어서 죽고 싶다는 생각이 들어").detected()).isTrue();
    }

    @Test
    void 띄어쓰기가_달라도_감지한다() {
        assertThat(service.check("죽고싶어").detected()).isTrue();
    }

    @Test
    void 일상적인_고민은_감지하지_않는다() {
        assertThat(service.check("친구가 3시간째 답장이 없어").detected()).isFalse();
    }

    @Test
    void 빈_글은_감지하지_않는다() {
        assertThat(service.check("  ").detected()).isFalse();
    }

    @Test
    void 안내_목록에_109와_마들랜이_있다() {
        assertThat(service.getGuides())
                .extracting(SafetyGuideResponse::guideType)
                .containsExactly(GuideType.HOTLINE_109, GuideType.SNS_COUNSEL);
    }
}
