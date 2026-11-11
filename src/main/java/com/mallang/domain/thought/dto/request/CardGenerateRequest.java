package com.mallang.domain.thought.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CardGenerateRequest(
        @NotBlank @Size(max = 2000) String content
) {
    // 로그에 사용자의 글이 찍히지 않도록 내용을 가린다
    @Override
    public String toString() {
        return "CardGenerateRequest[content=***]";
    }
}