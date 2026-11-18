package com.mallang.domain.safety.service;


import com.mallang.domain.safety.dto.response.SafetyGuideResponse;
import java.util.List;
import com.mallang.domain.safety.dto.response.SafetyCheckResponse;
import com.mallang.domain.safety.enums.GuideType;
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
                ? SafetyCheckResponse.detected(GuideType.CRISIS)
                : SafetyCheckResponse.safe();
    }

    // "죽고 싶다", "죽고싶다!" 처럼 띄어쓰기와 부호가 달라도 같게 보도록 정리
    private String normalize(String text) {
        return text.replaceAll("[\\s\\p{Punct}]", "").toLowerCase();
    }
    public List<SafetyGuideResponse> getGuides() {
        // 연락 방식과 URL은 출시 전 공식 안내로 다시 확인해야 한다 (확인 전 URL은 null)
        return List.of(
                new SafetyGuideResponse(GuideType.CRISIS, "자살예방 상담전화", "109", null),
                new SafetyGuideResponse(GuideType.CRISIS, "마들랜 (자살예방 SNS 상담)",
                        "카카오톡에서 '마들랜' 검색, 문자 109, 마들랜 앱", null)
        );
    }

}


