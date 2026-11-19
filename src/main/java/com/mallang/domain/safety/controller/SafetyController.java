package com.mallang.domain.safety.controller;

import com.mallang.common.response.ApiResponse;
import com.mallang.domain.safety.dto.request.SafetyCheckRequest;
import com.mallang.domain.safety.dto.response.SafetyCheckResponse;
import com.mallang.domain.safety.dto.response.SafetyGuideListResponse;
import com.mallang.domain.safety.service.SafetyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/safety")
public class SafetyController {

    private final SafetyService safetyService;

    public SafetyController(SafetyService safetyService) {
        this.safetyService = safetyService;
    }

    @PostMapping("/check")
    public ApiResponse<SafetyCheckResponse> check(@Valid @RequestBody SafetyCheckRequest request) {
        return ApiResponse.onSuccess(safetyService.check(request.text()));
    }

    @GetMapping("/guides")
    public ApiResponse<SafetyGuideListResponse> guides() {
        return ApiResponse.onSuccess(new SafetyGuideListResponse(safetyService.getGuides()));
    }
}