package com.mallang.domain.reflection.enums;

public enum GrowthStage {
    TANGLED("엉킨 털"),
    UNTANGLED("풀린 털"),
    FLUFFY("몽실 털"),
    CLOUD("구름 같은 털");

    private final String label;

    GrowthStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}