package com.mallang.domain.path.service;

import com.mallang.domain.path.dto.request.PathRecommendRequest;
import com.mallang.domain.path.dto.response.PathRecommendResponse;
import com.mallang.domain.path.enums.CanAct;
import com.mallang.domain.path.enums.PathType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PathService {

    // 초기 마음 무게가 이 값 이상이면 "무겁다"고 본다 (명세: 4~5)
    private static final int HEAVY_INITIAL_WEIGHT = 4;

    // initialWeight는 서버에 저장된 세션의 초기 마음 무게를 호출하는 쪽에서 넘겨준다
    public PathRecommendResponse recommend(PathRecommendRequest request, int initialWeight) {
        PathType recommended = decide(request.canAct(), request.needsTime(), initialWeight);
        return new PathRecommendResponse(recommended, List.of(PathType.values()));
    }

    // 명세의 Q4, Q5, 초기 무게 표를 그대로 옮긴 규칙
    PathType decide(CanAct canAct, boolean needsTime, int initialWeight) {
        boolean heavy = initialWeight >= HEAVY_INITIAL_WEIGHT;

        if (canAct == CanAct.HAS_ACTION) {
            return heavy ? PathType.SWITCH : PathType.ACTION;
        }
        if (needsTime) {
            return PathType.DEFER;
        }
        return heavy ? PathType.SWITCH : PathType.DEFER;
    }
}