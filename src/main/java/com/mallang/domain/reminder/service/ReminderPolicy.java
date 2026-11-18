package com.mallang.domain.reminder.service;

import com.mallang.domain.reminder.dto.response.ReminderStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Pure rules for reminder order, notice visibility, and the five-record limit. */
public final class ReminderPolicy {
    public static final int MAX_RECORDS = 5;

    public record Entry(long id, Instant createdAt) {
        public Entry {
            if (id <= 0) {
                throw new IllegalArgumentException("id must be positive");
            }
            Objects.requireNonNull(createdAt, "createdAt");
        }
    }

    private static final Comparator<Entry> OLDEST_FIRST = Comparator
            .comparing(Entry::createdAt)
            .thenComparingLong(Entry::id);

    private ReminderPolicy() {}

    public static ReminderStatus status(long count) {
        if (count < 0 || count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid reminder count");
        }
        return new ReminderStatus((int) count, count > 0);
    }

    /** Call after insertion under a user lock in the same transaction. Deletes reminder rows only. */
    public static List<Long> idsToEvict(List<Entry> entries) {
        Objects.requireNonNull(entries, "entries");
        long distinctIds = entries.stream().map(Entry::id).distinct().count();
        if (distinctIds != entries.size()) {
            throw new IllegalArgumentException("duplicate reminder ID");
        }
        return entries.stream()
                .sorted(OLDEST_FIRST)
                .limit(Math.max(0, entries.size() - MAX_RECORDS))
                .map(Entry::id)
                .toList();
    }

    public static List<Entry> newestFirst(List<Entry> entries) {
        Objects.requireNonNull(entries, "entries");
        return entries.stream().sorted(OLDEST_FIRST.reversed()).toList();
    }
}
