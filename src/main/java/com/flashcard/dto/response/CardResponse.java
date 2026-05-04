package com.flashcard.dto.response;

import com.flashcard.entity.Card;

import java.time.LocalDateTime;

public record CardResponse(
        Long id,
        String term,
        String definition,
        Long deckId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getTerm(),
                card.getDefinition(),
                card.getDeck().getId(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
