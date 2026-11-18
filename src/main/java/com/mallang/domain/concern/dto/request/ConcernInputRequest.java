package com.mallang.domain.concern.dto.request;

import com.mallang.domain.concern.enums.SituationType;

public record ConcernInputRequest(SituationType situationType, Integer weightBefore, String rawText) {
    @Override
    public String toString() {
        return "ConcernInputRequest[situationType=" + situationType
                + ", weightBefore=" + weightBefore + ", rawText=***]";
    }
}
