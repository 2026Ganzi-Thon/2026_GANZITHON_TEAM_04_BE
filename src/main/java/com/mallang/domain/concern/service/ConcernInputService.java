package com.mallang.domain.concern.service;

import com.mallang.domain.concern.dto.request.ConcernInputRequest;
import com.mallang.domain.concern.dto.response.ConcernInputResult;
import com.mallang.domain.safety.dto.response.SafetyCheckResponse;
import com.mallang.domain.safety.service.SafetyService;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.mallang.common.exception.GeneralException;

/** Validates the temporary input before the card step. */
@Service
public class ConcernInputService {
    public static final int MAX_CHARACTERS = 200;
    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    private final SafetyService safetyService;

    public ConcernInputService(SafetyService safetyService) {
        this.safetyService = safetyService;
    }

    public ConcernInputResult prepare(ConcernInputRequest request) {
        if (request == null || request.situationType() == null || request.weightBefore() == null
                || request.weightBefore() < 1 || request.weightBefore() > 5) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "상황과 마음의 무게(1~5)를 선택해 주세요.");
        }
        String text = request.rawText();
        if (text == null || text.codePoints().allMatch(
                c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "고민을 입력해 주세요.");
        }
        if (GRAPHEME.matcher(text).results().limit(MAX_CHARACTERS + 1L).count() > MAX_CHARACTERS) {
            throw new GeneralException(HttpStatus.BAD_REQUEST, "고민은 200자 이내로 입력해 주세요.");
        }
        SafetyCheckResponse safety = safetyService.check(text);
        return new ConcernInputResult(request.situationType(), request.weightBefore(),
                safety.detected() ? null : text, safety);
    }
}
