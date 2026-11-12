package com.mallang.domain.thought.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mallang.domain.thought.exception.CardGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiCardGeneratorTest {

    private MockRestServiceServer server;
    private AiCardGenerator generator;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.example.test/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        generator = new AiCardGenerator(builder.build(), "test-model", true);
    }

    private void respondWith(String content) {
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + content + "\"}}]}";
        server.expect(requestTo("https://api.example.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    void AI_응답을_줄_단위_카드로_바꾼다() {
        respondWith("답장이 3시간째 없어\\n화났나 봐");

        assertThat(generator.generate("원문")).containsExactly("답장이 3시간째 없어", "화났나 봐");
    }

    @Test
    void 목록_기호는_지우고_숫자로_시작하는_문장은_유지한다() {
        respondWith("- 답장이 없어\\n2. 화났나 봐\\n3시간째 걱정돼");

        assertThat(generator.generate("원문")).containsExactly("답장이 없어", "화났나 봐", "3시간째 걱정돼");
    }

    @Test
    void 응답이_비어_있으면_예외가_난다() {
        server.expect(requestTo("https://api.example.test/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator.generate("원문")).isInstanceOf(CardGenerationException.class);
    }

    @Test
    void 설정이_없으면_예외가_난다() {
        AiCardGenerator unconfigured = new AiCardGenerator(RestClient.create(), "", false);

        assertThatThrownBy(() -> unconfigured.generate("원문")).isInstanceOf(CardGenerationException.class);
    }
}