package com.mallang.domain.concern.enums;

public enum SituationType {
    RELATIONSHIP,
    STUDY_OR_WORK,
    WAITING_FOR_RESULT,
    OTHER;

    public String apiValue() {
        return switch (this) {
            case STUDY_OR_WORK -> "STUDY_WORK";
            case WAITING_FOR_RESULT -> "WAITING";
            default -> name();
        };
    }

    public static SituationType fromApi(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "STUDY_WORK", "STUDY_OR_WORK" -> STUDY_OR_WORK;
            case "WAITING", "WAITING_FOR_RESULT" -> WAITING_FOR_RESULT;
            case "RELATIONSHIP" -> RELATIONSHIP;
            case "OTHER" -> OTHER;
            default -> null;
        };
    }
}
