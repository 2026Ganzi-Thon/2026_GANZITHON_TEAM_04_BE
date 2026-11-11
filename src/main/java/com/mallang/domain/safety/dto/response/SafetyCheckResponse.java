package com.mallang.domain.safety.dto.response;

import com.mallang.domain.safety.enums.GuideType;

public record SafetyCheckResponse(boolean detected, GuideType guideType) {

    public static SafetyCheckResponse safe() {
        return new SafetyCheckResponse(false, null);
    }

    public static SafetyCheckResponse detected(GuideType guideType) {
        return new SafetyCheckResponse(true, guideType);
    }
}