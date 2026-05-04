package com.flashcard.dto.response;

import com.flashcard.entity.Deck;

import java.time.LocalDateTime;

public record DeckResponse(
        Long id,
        String title,
        String description,
        int cardCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DeckResponse from(Deck deck) {
        return new DeckResponse(
                deck.getId(),
                deck.getTitle(),
                deck.getDescription(),
                deck.getCards().size(),
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }
}
