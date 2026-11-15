package com.mallang.domain.thought.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class UpstageThoughtClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private MockRestServiceServer server;
    private UpstageThoughtClient client;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UpstageThoughtClient(builder.build(), mapper, "solar-mini4", true);
    }

    private void respond(String cardsJson, String finishReason) throws Exception {
        String response = mapper.writeValueAsString(Map.of("choices", List.of(Map.of(
                "finish_reason", finishReason,
                "message", Map.of("content", cardsJson)))));
        server.expect(requestTo("https://example.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("solar-mini4"))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    @Test
    void validStructuredCardsPreserveOrder() throws Exception {
        respond("{\"cards\":[{\"text\":\"답장이 없어\"},{\"text\":\"화났나 봐\"}]}", "stop");
        assertThat(client.split("친구에게 답장이 없어. 화났나 봐."))
                .containsExactly("답장이 없어", "화났나 봐");
        server.verify();
    }

    @Test
    void duplicateCardsAreRejected() throws Exception {
        respond("{\"cards\":[{\"text\":\"걱정돼\"},{\"text\":\"걱정돼\"}]}", "stop");
        assertThatThrownBy(() -> client.split("걱정돼"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Thought card generation failed");
    }

    @Test
    void incompleteResponseIsRejected() throws Exception {
        respond("{\"cards\":[{\"text\":\"걱정돼\"}]}", "length");
        assertThatThrownBy(() -> client.split("걱정돼"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingKeyDoesNotCallProvider() {
        var unconfigured = new UpstageThoughtClient(RestClient.create(), mapper, "solar-mini4", false);
        assertThatThrownBy(() -> unconfigured.split("고민"))
                .isInstanceOf(IllegalStateException.class);
    }
}
