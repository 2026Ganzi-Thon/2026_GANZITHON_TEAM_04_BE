package com.mallang.domain.safety.dto.response;

import com.mallang.domain.safety.enums.GuideType;

public record SafetyGuideResponse(
        GuideType guideType,
        String name,
        String contact,
        String description
) {
}