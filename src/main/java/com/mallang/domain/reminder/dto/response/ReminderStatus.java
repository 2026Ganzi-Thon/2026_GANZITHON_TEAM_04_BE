package com.mallang.domain.reminder.dto.response;

/** Main-screen in-app notice state. */
public record ReminderStatus(int count, boolean showNotice) {
    public ReminderStatus {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
        if (showNotice != (count > 0)) {
            throw new IllegalArgumentException("notice must match count");
        }
    }
}
