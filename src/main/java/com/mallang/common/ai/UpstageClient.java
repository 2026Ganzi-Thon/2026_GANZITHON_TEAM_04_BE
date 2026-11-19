package com.mallang.common.ai;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UpstageClient {
    private final RestClient client;
    private final String model;

    @Autowired
    public UpstageClient(Environment environment) {
        this(configuredClient(environment), environment.getProperty("UPSTAGE_MODEL"));
    }

    public UpstageClient(RestClient client, String model) {
        this.client = client;
        this.model = model;
    }

    public String complete(String instructions, String input) {
        if (client == null || model == null || model.isBlank()) {
            throw new Unavailable();
        }
        var request = new ChatRequest(model, List.of(new Message("system", instructions), new Message("user", input)), 0.0);
        try {
            ChatResponse response = client.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().body(ChatResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new Unavailable();
            }
            Choice choice = response.choices().getFirst();
            if (choice == null || choice.message() == null || choice.message().content() == null
                    || choice.message().content().isBlank()) {
                throw new Unavailable();
            }
            return choice.message().content();
        } catch (RestClientException exception) {
            // Provider errors may contain the private input.
            throw new Unavailable();
        }
    }

    private static RestClient configuredClient(Environment environment) {
        String baseUrl = environment.getProperty("UPSTAGE_BASE_URL");
        String apiKey = environment.getProperty("UPSTAGE_API_KEY");
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            return null;
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).build();
    }

    private record ChatRequest(String model, List<Message> messages, double temperature) {}
    private record Message(String role, String content) {}
    private record ChatResponse(List<Choice> choices) {}
    private record Choice(Message message) {}

    public static class Unavailable extends RuntimeException {
        private Unavailable() {
            super("AI 응답을 사용할 수 없습니다.");
        }
    }
}
