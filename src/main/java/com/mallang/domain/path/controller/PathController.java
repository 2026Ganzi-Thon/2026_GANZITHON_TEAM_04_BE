package com.mallang.domain.path.controller;

import com.mallang.common.exception.GeneralException;
import com.mallang.common.response.ApiResponse;
import com.mallang.domain.path.dto.response.ActivityListResponse;
import com.mallang.domain.path.enums.PathType;
import com.mallang.domain.path.service.PathActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/path")
public class PathController {

    private final PathActivityService pathActivityService;

    public PathController(PathActivityService pathActivityService) {
        this.pathActivityService = pathActivityService;
    }

    @GetMapping("/actions")
    public ApiResponse<ActivityListResponse> actions(@RequestParam PathType path) {
        // 명세: 활동 목록은 SWITCH만 제공하고 ACTION 등은 이 API로 제공하지 않는다
        if (path != PathType.SWITCH) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "SWITCH 경로만 지원합니다.");
        }
        return ApiResponse.onSuccess(pathActivityService.pickSwitchActivities());
    }
}