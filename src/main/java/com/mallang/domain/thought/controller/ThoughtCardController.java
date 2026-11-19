package com.mallang.domain.thought.controller;

import com.mallang.common.response.ApiResponse;
import com.mallang.domain.concern.service.AnonymousUserService;
import com.mallang.domain.thought.dto.response.CardGenerateResponse;
import com.mallang.domain.thought.dto.response.CardResponse;
import com.mallang.domain.thought.service.ThoughtCardService;
import com.mallang.domain.thought.service.ThoughtCardService.Classification;
import com.mallang.domain.thought.service.ThoughtCardService.EditedCard;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "anonymousBearer")
@RequestMapping("/api/v1")
public class ThoughtCardController {
    private final AnonymousUserService users;
    private final ThoughtCardService cards;

    public ThoughtCardController(AnonymousUserService users, ThoughtCardService cards) {
        this.users = users;
        this.cards = cards;
    }

    @PostMapping({"/concerns/{sessionId}/cards/generate", "/sessions/{sessionId}/thought-cards"})
    public ApiResponse<CardGenerateResponse> generate(
            @RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable String sessionId) {
        return ApiResponse.onSuccess(cards.generate(users.requireUser(authorization), sessionId));
    }

    @GetMapping("/concerns/{sessionId}/cards")
    public ApiResponse<CardList> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String sessionId) {
        return ApiResponse.onSuccess(new CardList(cards.list(users.requireUser(authorization), sessionId)));
    }

    @PostMapping("/concerns/{sessionId}/cards")
    public ApiResponse<CardItem> add(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String sessionId, @RequestBody CardInput request) {
        return ApiResponse.onSuccess(new CardItem(cards.add(users.requireUser(authorization), sessionId, request.content())));
    }

    @PatchMapping("/cards/{cardId}")
    public ApiResponse<EditedCard> update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable long cardId, @RequestBody CardInput request) {
        return ApiResponse.onSuccess(cards.update(users.requireUser(authorization), cardId, request.content()));
    }

    @DeleteMapping("/cards/{cardId}")
    public ApiResponse<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable long cardId) {
        cards.delete(users.requireUser(authorization), cardId);
        return ApiResponse.onSuccess();
    }

    @PutMapping("/concerns/{sessionId}/cards/classification")
    public ApiResponse<CardList> classify(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable String sessionId, @Valid @RequestBody Classifications request) {
        return ApiResponse.onSuccess(new CardList(cards.classify(users.requireUser(authorization), sessionId, request.items())));
    }

    public record CardList(List<CardResponse> cards) {}
    public record CardItem(CardResponse card) {}
    public record CardInput(String content) {
        @Override public String toString() { return "CardInput[content=***]"; }
    }
    public record Classifications(@NotNull @Size(max = 8) List<Classification> items) {}
}
