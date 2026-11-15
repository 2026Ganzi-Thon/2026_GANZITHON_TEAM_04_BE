package com.mallang.domain.reminder.dto.response;

import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.path.enums.PathType;
import java.time.Instant;

/** Only the approved completed-result fields are exposed. */
public record ReminderSummary(
        long reminderId,
        String sessionId,
        Instant completedAt,
        SituationType situationType,
        int weightBefore,
        PathType recommendedRoute,
        PathType chosenRoute,
        boolean weightReduced
) {
}
