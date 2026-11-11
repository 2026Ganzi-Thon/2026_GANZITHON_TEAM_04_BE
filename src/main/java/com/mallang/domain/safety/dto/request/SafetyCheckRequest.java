package com.mallang.domain.safety.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SafetyCheckRequest(@NotBlank String text) {

    // 로그에 사용자의 글이 찍히지 않도록 내용을 가린다
    @Override
    public String toString() {
        return "SafetyCheckRequest[text=***]";
    }
}