package com.mallang.domain.path.enums;

public enum SwitchActivity {
    BREATHE("천천히 숨 쉬기", "편안한 속도로 숨을 들이쉬고 내쉬어 보세요."),
    WATER("물 한 잔 마시기", "마실 수 있다면 물을 조금 마셔 보세요."),
    STRETCH("몸 가볍게 움직이기", "편안한 범위에서 어깨나 몸을 움직여 보세요."),
    LOOK_AROUND("주변 살펴보기", "지금 보이는 것 세 가지를 천천히 살펴보세요."),
    WINDOW("창밖 보기", "잠시 주변이나 창밖을 바라보세요.");

    private final String title;
    private final String guide;

    SwitchActivity(String title, String guide) {
        this.title = title;
        this.guide = guide;
    }

    public String getTitle() {
        return title;
    }

    public String getGuide() {
        return guide;
    }
}