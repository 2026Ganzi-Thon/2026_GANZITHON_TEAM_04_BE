package com.mallang.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExceptionAdviceTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new ExceptionAdvice())
                .build();
    }

    @Test
    void returnsGeneralExceptionStatusAndMessage() throws Exception {
        mockMvc.perform(get("/tests/business-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("요청한 항목을 찾을 수 없습니다."));
    }

    @Test
    void rejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/tests/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/tests/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/tests/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("서버 오류가 발생했습니다."));
    }

    @Test
    void preservesFrameworkStatusAndAllowHeader() throws Exception {
        mockMvc.perform(get("/tests/request"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, "POST"))
                .andExpect(jsonPath("$.status").value(405));
    }

    // A non-static inner controller is not eligible for application component scanning.
    @RestController
    class TestController {

        @GetMapping("/tests/business-error")
        public void businessError() {
            throw new GeneralException(HttpStatus.NOT_FOUND, "요청한 항목을 찾을 수 없습니다.");
        }

        @GetMapping("/tests/unexpected-error")
        public void unexpectedError() {
            throw new IllegalStateException("Internal database details");
        }

        @PostMapping("/tests/request")
        public TestRequest request(@Valid @RequestBody TestRequest request) {
            return request;
        }
    }

    public record TestRequest(@NotBlank String name) {
    }

}
