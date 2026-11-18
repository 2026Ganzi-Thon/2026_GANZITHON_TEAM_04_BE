package com.mallang.domain.reminder.service;

import com.mallang.domain.reminder.service.ReminderPolicy.Entry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReminderPolicyTest {
    private static final Instant NOW = Instant.parse("2026-11-16T00:00:00Z");

    @Test void sixthRecordEvictsOnlyOldestReminder() {
        List<Entry> entries = List.of(
                new Entry(6, NOW), new Entry(5, NOW), new Entry(4, NOW),
                new Entry(3, NOW), new Entry(2, NOW), new Entry(1, NOW));
        assertEquals(List.of(1L), ReminderPolicy.idsToEvict(entries));
        assertEquals(List.of(6L, 5L, 4L, 3L, 2L, 1L),
                ReminderPolicy.newestFirst(entries).stream().map(Entry::id).toList());
    }

    @Test void noticeTracksCurrentCountIncludingLastDeletion() {
        assertFalse(ReminderPolicy.status(0).showNotice());
        assertTrue(ReminderPolicy.status(1).showNotice());
        assertEquals(1, ReminderPolicy.status(1).count());
        assertEquals(List.of(), ReminderPolicy.idsToEvict(List.of()));
    }

    @Test void olderTimeWinsBeforeIdTieBreaker() {
        var older = new Entry(99, NOW.minusSeconds(1));
        var sameTime = List.of(new Entry(1, NOW), new Entry(2, NOW),
                new Entry(3, NOW), new Entry(4, NOW), new Entry(5, NOW));
        var entries = new java.util.ArrayList<>(sameTime);
        entries.add(older);
        assertEquals(List.of(99L), ReminderPolicy.idsToEvict(entries));
    }
}
