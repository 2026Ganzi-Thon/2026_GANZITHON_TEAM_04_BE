package com.mallang.domain.path.service;

import com.mallang.domain.path.dto.response.ActivityListResponse;
import com.mallang.domain.path.dto.response.ActivityResponse;
import com.mallang.domain.path.enums.SwitchActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class PathActivityService {

    private static final int PICK_COUNT = 3;

    // 잠시 전환하기: 활동 중 중복 없이 무작위 3개
    public ActivityListResponse pickSwitchActivities() {
        return pickSwitchActivities(ThreadLocalRandom.current());
    }

    // 테스트에서 같은 결과를 재현할 수 있도록 Random을 받는 버전
    ActivityListResponse pickSwitchActivities(Random random) {
        List<SwitchActivity> all = new ArrayList<>(List.of(SwitchActivity.values()));
        Collections.shuffle(all, random);

        List<ActivityResponse> picked = all.stream()
                .limit(PICK_COUNT)
                .map(activity -> new ActivityResponse(
                        activity.name(), activity.getTitle(), activity.getGuide()))
                .toList();
        return new ActivityListResponse(picked);
    }
}