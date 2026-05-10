package com.flashcard.service;

import com.flashcard.dto.request.CreateCardRequest;
import com.flashcard.dto.request.UpdateCardRequest;
import com.flashcard.dto.response.CardResponse;
import com.flashcard.entity.Card;
import com.flashcard.entity.Deck;
import com.flashcard.exception.DuplicateResourceException;
import com.flashcard.exception.ResourceNotFoundException;
import com.flashcard.repository.CardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final DeckService deckService;

    public CardService(CardRepository cardRepository, DeckService deckService) {
        this.cardRepository = cardRepository;
        this.deckService = deckService;
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> listCards(Long deckId, String ownerEmail,
                                        String search, Pageable pageable) {
        deckService.findDeckAndVerifyOwnership(deckId, ownerEmail);

        Page<Card> cards;
        if (search != null && !search.isBlank()) {
            cards = cardRepository.findByDeckIdAndTermContainingIgnoreCase(
                    deckId, search, pageable);
        } else {
            cards = cardRepository.findByDeckId(deckId, pageable);
        }
        return cards.map(CardResponse::from);
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(Long deckId, Long cardId, String ownerEmail) {
        deckService.findDeckAndVerifyOwnership(deckId, ownerEmail);
        Card card = findCardInDeck(cardId, deckId);
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse createCard(Long deckId, CreateCardRequest request, String ownerEmail) {
        Deck deck = deckService.findDeckAndVerifyOwnership(deckId, ownerEmail);

        if (cardRepository.existsByTermAndDeckId(request.term(), deckId)) {
            throw new DuplicateResourceException(
                    "Card with term '" + request.term() + "' already exists in this deck");
        }

        Card card = new Card();
        card.setTerm(request.term());
        card.setDefinition(request.definition());
        card.setDeck(deck);
        card = cardRepository.save(card);

        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse updateCard(Long deckId, Long cardId,
                                   UpdateCardRequest request, String ownerEmail) {
        deckService.findDeckAndVerifyOwnership(deckId, ownerEmail);
        Card card = findCardInDeck(cardId, deckId);

        if (!card.getTerm().equals(request.term())
                && cardRepository.existsByTermAndDeckId(request.term(), deckId)) {
            throw new DuplicateResourceException(
                    "Card with term '" + request.term() + "' already exists in this deck");
        }

        card.setTerm(request.term());
        card.setDefinition(request.definition());
        card = cardRepository.save(card);

        return CardResponse.from(card);
    }

    @Transactional
    public void deleteCard(Long deckId, Long cardId, String ownerEmail) {
        deckService.findDeckAndVerifyOwnership(deckId, ownerEmail);
        Card card = findCardInDeck(cardId, deckId);
        cardRepository.delete(card);
    }

    @Transactional(readOnly = true)
    public Optional<CardResponse> getRandomCard(Long deckId, String ownerEmail) {
        deckService.findDeckAndVerifyOwnership(deckId, ownerEmail);

        long count = cardRepository.countByDeckId(deckId);
        if (count == 0) {
            return Optional.empty();
        }

        int randomIndex = ThreadLocalRandom.current().nextInt((int) count);
        Page<Card> page = cardRepository.findByDeckId(deckId, PageRequest.of(randomIndex, 1));
        Card card = page.getContent().get(0);

        return Optional.of(CardResponse.from(card));
    }

    private Card findCardInDeck(Long cardId, Long deckId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        if (!card.getDeck().getId().equals(deckId)) {
            throw new ResourceNotFoundException("Card not found in this deck");
        }
        return card;
    }
}
