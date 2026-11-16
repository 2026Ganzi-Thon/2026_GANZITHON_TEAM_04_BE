package com.mallang.domain.reminder.controller;

import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.reminder.dto.response.ReminderStatus;
import com.mallang.domain.reminder.dto.response.ReminderSummary;
import com.mallang.domain.reminder.service.ReminderService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {
    private final AnonymousUserService users;
    private final ReminderService reminders;

    public ReminderController(AnonymousUserService users, ReminderService reminders) {
        this.users = users;
        this.reminders = reminders;
    }

    @GetMapping("/status")
    public ReminderStatus status(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return reminders.status(users.requireUser(authorization));
    }

    @GetMapping
    public List<ReminderSummary> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return reminders.list(users.requireUser(authorization));
    }

    @GetMapping("/{reminderId}")
    public ReminderSummary detail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable long reminderId) {
        return reminders.detail(users.requireUser(authorization), reminderId);
    }

    @DeleteMapping("/{reminderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                       @PathVariable long reminderId) {
        reminders.delete(users.requireUser(authorization), reminderId);
    }
}
