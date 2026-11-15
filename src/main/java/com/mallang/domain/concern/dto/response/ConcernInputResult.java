package com.mallang.domain.concern.dto.response;

import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.safety.dto.response.SafetyCheckResponse;
import java.util.Objects;

/** Temporary handoff to the card step; callers must not persist or log rawText. */
public record ConcernInputResult(
        SituationType situationType,
        int weightBefore,
        String rawText,
        SafetyCheckResponse safety
) {
    public ConcernInputResult {
        Objects.requireNonNull(safety, "safety");
        if (safety.detected() && rawText != null) {
            throw new IllegalArgumentException("crisis input must not be handed to cards");
        }
        if (!safety.detected() && rawText == null) {
            throw new IllegalArgumentException("safe input requires text for card handoff");
        }
    }

    @Override
    public String toString() {
        return "ConcernInputResult[situationType=" + situationType
                + ", weightBefore=" + weightBefore + ", rawText=***, safety=" + safety + "]";
    }
}
