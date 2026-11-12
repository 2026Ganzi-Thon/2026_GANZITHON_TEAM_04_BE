package com.mallang.domain.path.dto.request;

import com.mallang.domain.path.enums.CanAct;
import jakarta.validation.constraints.NotNull;

public record PathRecommendRequest(
        @NotNull Long coreCardId,
        @NotNull CanAct canAct,
        @NotNull Boolean needsTime
) {
}