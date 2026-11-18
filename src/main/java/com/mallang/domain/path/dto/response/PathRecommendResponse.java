package com.mallang.domain.path.dto.response;

import com.mallang.domain.path.enums.PathType;
import java.util.List;

public record PathRecommendResponse(
        PathType recommendedPath,
        List<PathType> options
) {
}