package com.mallang.domain.thought.service;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleCardGenerator implements CardGenerator {

    // 공백 없이 이어지는 문장도 나누고, 소수점이나 연속 문장부호는 유지한다.
    private static final String SPLIT_REGEX = "(?<=[.!?。！？])(?:\\s+|(?=[^\\s.!?。！？\\d]))|\\R+";

    @Override
    public List<String> generate(String content) {
        return Arrays.stream(content.split(SPLIT_REGEX))
                .map(String::trim)
                .filter(sentence -> !sentence.isEmpty())
                .toList();
    }
}
