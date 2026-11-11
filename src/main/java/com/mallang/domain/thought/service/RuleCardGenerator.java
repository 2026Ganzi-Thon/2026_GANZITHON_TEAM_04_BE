package com.mallang.domain.thought.service;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleCardGenerator implements CardGenerator {

    // 줄바꿈이나 문장 부호(. ! ?) 뒤의 공백을 기준으로 나눈다
    private static final String SPLIT_REGEX = "(?<=[.!?])\\s+|\\R+";

    @Override
    public List<String> generate(String content) {
        return Arrays.stream(content.split(SPLIT_REGEX))
                .map(String::trim)
                .filter(sentence -> !sentence.isEmpty())
                .toList();
    }
}