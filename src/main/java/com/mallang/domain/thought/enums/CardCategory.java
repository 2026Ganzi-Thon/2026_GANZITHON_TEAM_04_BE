package com.mallang.domain.thought.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CardCategory {
    FACT, ASSUMPTION, UNCLASSIFIED;

    @JsonCreator
    public static CardCategory fromJson(String value) {
        return valueOf(value);
    }
}
