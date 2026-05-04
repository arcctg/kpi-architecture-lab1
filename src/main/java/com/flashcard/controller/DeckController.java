package com.flashcard.controller;

import com.flashcard.dto.request.CreateDeckRequest;
import com.flashcard.dto.request.UpdateDeckRequest;
import com.flashcard.dto.response.DeckResponse;
import com.flashcard.service.DeckService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public Page<DeckResponse> listDecks(@AuthenticationPrincipal UserDetails user,
                                        Pageable pageable) {
        return deckService.listDecks(user.getUsername(), pageable);
    }

    @GetMapping("/{id}")
    public DeckResponse getDeck(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails user) {
        return deckService.getDeck(id, user.getUsername());
    }

    @PostMapping
    public ResponseEntity<DeckResponse> createDeck(
            @Valid @RequestBody CreateDeckRequest request,
            @AuthenticationPrincipal UserDetails user) {
        DeckResponse response = deckService.createDeck(request, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public DeckResponse updateDeck(@PathVariable Long id,
                                   @Valid @RequestBody UpdateDeckRequest request,
                                   @AuthenticationPrincipal UserDetails user) {
        return deckService.updateDeck(id, request, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeck(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails user) {
        deckService.deleteDeck(id, user.getUsername());
    }
}
