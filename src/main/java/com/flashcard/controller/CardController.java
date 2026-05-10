package com.flashcard.controller;

import com.flashcard.dto.request.CreateCardRequest;
import com.flashcard.dto.request.UpdateCardRequest;
import com.flashcard.dto.response.CardResponse;
import com.flashcard.service.CardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/decks/{deckId}/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public Page<CardResponse> listCards(@PathVariable Long deckId,
                                        @RequestParam(required = false) String search,
                                        @AuthenticationPrincipal UserDetails user,
                                        Pageable pageable) {
        return cardService.listCards(deckId, user.getUsername(), search, pageable);
    }

    @GetMapping("/random")
    public ResponseEntity<CardResponse> getRandomCard(@PathVariable Long deckId,
                                                       @AuthenticationPrincipal UserDetails user) {
        Optional<CardResponse> card = cardService.getRandomCard(deckId, user.getUsername());
        return card.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{cardId}")
    public CardResponse getCard(@PathVariable Long deckId,
                                @PathVariable Long cardId,
                                @AuthenticationPrincipal UserDetails user) {
        return cardService.getCard(deckId, cardId, user.getUsername());
    }

    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @PathVariable Long deckId,
            @Valid @RequestBody CreateCardRequest request,
            @AuthenticationPrincipal UserDetails user) {
        CardResponse response = cardService.createCard(deckId, request, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{cardId}")
    public CardResponse updateCard(@PathVariable Long deckId,
                                   @PathVariable Long cardId,
                                   @Valid @RequestBody UpdateCardRequest request,
                                   @AuthenticationPrincipal UserDetails user) {
        return cardService.updateCard(deckId, cardId, request, user.getUsername());
    }

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long deckId,
                           @PathVariable Long cardId,
                           @AuthenticationPrincipal UserDetails user) {
        cardService.deleteCard(deckId, cardId, user.getUsername());
    }
}
