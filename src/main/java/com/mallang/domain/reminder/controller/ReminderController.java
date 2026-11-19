package com.mallang.domain.reminder.controller;

import com.mallang.common.response.ApiResponse;
import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.reflection.dto.ReflectionSummary;
import com.mallang.domain.reminder.dto.response.ReminderSummary;
import com.mallang.domain.reminder.service.ReminderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "anonymousBearer")
@RequestMapping("/api/v1/reminders")
public class ReminderController {
    private final AnonymousUserService users;
    private final ReminderService reminders;

    public ReminderController(AnonymousUserService users, ReminderService reminders) {
        this.users = users;
        this.reminders = reminders;
    }

    @GetMapping("/status")
    public ApiResponse<Notice> status(@RequestHeader(value = "Authorization", required = false) String authorization) {
        var status = reminders.status(users.requireUser(authorization));
        return ApiResponse.onSuccess(new Notice(status.count(), status.showNotice()));
    }

    @GetMapping
    public ApiResponse<ReminderList> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        var items = reminders.list(users.requireUser(authorization)).stream().map(this::item).toList();
        return ApiResponse.onSuccess(new ReminderList(items));
    }

    @GetMapping("/{reminderId}")
    public ApiResponse<ReminderItem> detail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @PathVariable long reminderId) {
        return ApiResponse.onSuccess(item(reminders.detail(users.requireUser(authorization), reminderId)));
    }

    @DeleteMapping("/{reminderId}")
    public ApiResponse<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable long reminderId) {
        reminders.delete(users.requireUser(authorization), reminderId);
        return ApiResponse.onSuccess();
    }

    private ReminderItem item(ReminderSummary row) {
        return new ReminderItem(row.reminderId(), new ReflectionSummary(row.reflectionId(), row.completedAt(),
                row.situationType().apiValue(), row.weightBefore(), row.recommendedRoute(), row.chosenRoute(), row.weightReduced()));
    }

    public record Notice(long count, boolean showNotification) {}
    public record ReminderItem(long reminderId, ReflectionSummary reflection) {}
    public record ReminderList(List<ReminderItem> items) {}
}
