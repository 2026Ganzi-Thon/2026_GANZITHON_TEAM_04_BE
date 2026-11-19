package com.mallang.domain.reminder.controller;

import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.reminder.service.ReminderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** FR-17: deleting the completed record also removes its reminder. */
@RestController
@SecurityRequirement(name = "anonymousBearer")
@RequestMapping("/api/v1/records")
public class CompletedRecordDeletionController {
    private final AnonymousUserService users;
    private final ReminderService reminders;

    public CompletedRecordDeletionController(AnonymousUserService users, ReminderService reminders) {
        this.users = users;
        this.reminders = reminders;
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                       @PathVariable String sessionId) {
        reminders.deleteCompletedRecord(users.requireUser(authorization), sessionId);
    }
}
