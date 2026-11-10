package com.mallang.domain.safety.service;

import com.mallang.domain.safety.dto.response.SafetyCheckResponse;
import com.mallang.domain.safety.enums.GuideType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SafetyService {

    // 공백과 특수문자를 뺀 상태에서 비교하는 기본 키워드 (초안)
    private static final List<String> CRISIS_KEYWORDS = List.of(
            "죽고싶", "죽어버리고싶", "자살", "자해",
            "사라지고싶", "살기싫", "삶을끝내", "목숨을끊"
    );

    public SafetyCheckResponse check(String text) {
        if (text == null || text.isBlank()) {
            return SafetyCheckResponse.safe();
        }
        String normalized = normalize(text);
        boolean detected = CRISIS_KEYWORDS.stream().anyMatch(normalized::contains);
        return detected
                ? SafetyCheckResponse.detected(GuideType.HOTLINE_109)
                : SafetyCheckResponse.safe();
    }

    // "죽고 싶다", "죽고싶다!" 처럼 띄어쓰기와 부호가 달라도 같게 보도록 정리
    private String normalize(String text) {
        return text.replaceAll("[\\s\\p{Punct}]", "").toLowerCase();
    }
}