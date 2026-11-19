package com.mallang.domain.reflection.dto;

import com.mallang.domain.path.enums.PathType;
import java.time.Instant;

public record ReflectionSummary(long reflectionId, Instant completedAt, String situationType, int initialWeight,
                                PathType recommendedPath, PathType selectedPath, boolean moodReduced) {}
