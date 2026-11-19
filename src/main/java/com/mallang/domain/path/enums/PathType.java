package com.mallang.domain.path.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PathType {
    ACTION,  // 행동하기
    SWITCH,  // 잠시 전환하기
    DEFER;   // 판단 미루기

    @JsonCreator
    public static PathType fromJson(String value) {
        return valueOf(value);
    }
}
