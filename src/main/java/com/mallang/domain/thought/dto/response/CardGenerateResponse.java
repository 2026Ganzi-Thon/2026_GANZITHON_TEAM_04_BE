package com.mallang.domain.thought.dto.response;

import com.mallang.domain.thought.enums.GeneratedBy;
import java.util.List;

public record CardGenerateResponse(List<CardResponse> cards, GeneratedBy generatedBy) {
}