package com.mallang.domain.reflection.service;

import com.mallang.domain.reflection.enums.GrowthStage;
import org.springframework.stereotype.Service;

@Service
public class GrowthService {

    // 명세: 0 엉킨 털, 1~2 풀린 털, 3~4 몽실 털, 5 이상 구름 같은 털
    public GrowthStage stageOf(int stepCount) {
        if (stepCount < 0) {
            throw new IllegalArgumentException("걸음 수는 0 이상이어야 합니다.");
        }
        if (stepCount == 0) {
            return GrowthStage.TANGLED;
        }
        if (stepCount <= 2) {
            return GrowthStage.UNTANGLED;
        }
        if (stepCount <= 4) {
            return GrowthStage.FLUFFY;
        }
        return GrowthStage.CLOUD;
    }
}