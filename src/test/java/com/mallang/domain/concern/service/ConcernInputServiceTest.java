package com.mallang.domain.concern.service;

import com.mallang.common.exception.GeneralException;
import com.mallang.domain.concern.dto.request.ConcernInputRequest;
import com.mallang.domain.concern.enums.SituationType;
import com.mallang.domain.safety.service.SafetyService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConcernInputServiceTest {
    private final ConcernInputService service = new ConcernInputService(new SafetyService());

    @Test void acceptsTwoHundredVisibleCharactersAndKeepsSelections() {
        String text = "👨‍👩‍👧‍👦".repeat(199) + " ";
        var result = service.prepare(new ConcernInputRequest(SituationType.RELATIONSHIP, 5, text));
        assertEquals(SituationType.RELATIONSHIP, result.situationType());
        assertEquals(5, result.weightBefore());
        assertEquals(text, result.rawText());
        assertFalse(result.toString().contains(text));
    }

    @Test void rejectsInvalidSelectionsAndText() {
        assertThrows(GeneralException.class, () -> service.prepare(new ConcernInputRequest(null, 1, "고민")));
        assertThrows(GeneralException.class, () -> service.prepare(new ConcernInputRequest(SituationType.OTHER, 0, "고민")));
        assertThrows(GeneralException.class, () -> service.prepare(new ConcernInputRequest(SituationType.OTHER, 6, "고민")));
        assertThrows(GeneralException.class, () -> service.prepare(new ConcernInputRequest(SituationType.OTHER, 3, " \n\u3000")));
        assertThrows(GeneralException.class, () -> service.prepare(new ConcernInputRequest(SituationType.OTHER, 3, "e\u0301".repeat(201))));
    }

    @Test void prioritizesExistingSafetyDetection() {
        var result = service.prepare(new ConcernInputRequest(SituationType.OTHER, 3, "죽고 싶어"));
        assertTrue(result.safety().detected());
        assertNull(result.rawText());
    }
}
