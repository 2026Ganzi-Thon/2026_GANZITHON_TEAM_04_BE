package com.mallang.domain.thought.dto.response;

import com.mallang.domain.thought.enums.CardCategory;

public record CardResponse(long cardId, int seq, String content, CardCategory category) {
    @Override
    public String toString() {
        return "CardResponse[cardId=" + cardId + ", seq=" + seq + ", category=" + category + "]";
    }
}
