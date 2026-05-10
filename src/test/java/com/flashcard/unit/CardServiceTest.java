package com.flashcard.unit;

import com.flashcard.dto.request.CreateCardRequest;
import com.flashcard.dto.request.UpdateCardRequest;
import com.flashcard.dto.response.CardResponse;
import com.flashcard.entity.Card;
import com.flashcard.entity.Deck;
import com.flashcard.entity.User;
import com.flashcard.exception.DuplicateResourceException;
import com.flashcard.exception.ResourceNotFoundException;
import com.flashcard.repository.CardRepository;
import com.flashcard.service.CardService;
import com.flashcard.service.DeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private DeckService deckService;

    @InjectMocks
    private CardService cardService;

    private User owner;
    private Deck deck;
    private Card card;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        deck = new Deck();
        deck.setId(10L);
        deck.setTitle("Java Basics");
        deck.setOwner(owner);

        card = new Card();
        card.setId(100L);
        card.setTerm("JVM");
        card.setDefinition("Java Virtual Machine");
        card.setDeck(deck);
    }

    @Test
    void listCards_withoutSearch() {
        var pageable = PageRequest.of(0, 10);
        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.findByDeckId(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(card)));

        Page<CardResponse> result = cardService.listCards(10L, "owner@example.com", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).term()).isEqualTo("JVM");
    }

    @Test
    void listCards_withSearch() {
        var pageable = PageRequest.of(0, 10);
        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.findByDeckIdAndTermContainingIgnoreCase(10L, "jvm", pageable))
                .thenReturn(new PageImpl<>(List.of(card)));

        Page<CardResponse> result = cardService.listCards(
                10L, "owner@example.com", "jvm", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void createCard_success() {
        var request = new CreateCardRequest("GC", "Garbage Collector");
        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.existsByTermAndDeckId("GC", 10L)).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card saved = inv.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        CardResponse response = cardService.createCard(10L, request, "owner@example.com");

        assertThat(response.term()).isEqualTo("GC");
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_duplicateTerm_throws409() {
        var request = new CreateCardRequest("JVM", "Duplicate");
        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.existsByTermAndDeckId("JVM", 10L)).thenReturn(true);

        assertThatThrownBy(() -> cardService.createCard(10L, request, "owner@example.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("JVM");
    }

    @Test
    void getCard_notFound_throws404() {
        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCard(10L, 999L, "owner@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCard_wrongDeck_throws404() {
        Card otherCard = new Card();
        otherCard.setId(200L);
        Deck otherDeck = new Deck();
        otherDeck.setId(99L);
        otherCard.setDeck(otherDeck);

        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.findById(200L)).thenReturn(Optional.of(otherCard));

        assertThatThrownBy(() -> cardService.getCard(10L, 200L, "owner@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCard_success() {
        var request = new UpdateCardRequest("JVM Updated", "Updated definition");
        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(cardRepository.existsByTermAndDeckId("JVM Updated", 10L)).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardResponse response = cardService.updateCard(
                10L, 100L, request, "owner@example.com");

        assertThat(response.term()).isEqualTo("JVM Updated");
    }

    @Test
    void deleteCard_success() {
        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.findById(100L)).thenReturn(Optional.of(card));

        cardService.deleteCard(10L, 100L, "owner@example.com");

        verify(cardRepository).delete(card);
    }

    @Test
    void getRandomCard_success() {
        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.countByDeckId(10L)).thenReturn(3L);
        when(cardRepository.findByDeckId(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(card)));

        Optional<CardResponse> result = cardService.getRandomCard(10L, "owner@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().term()).isEqualTo("JVM");
    }

    @Test
    void getRandomCard_emptyDeck_returnsEmpty() {
        when(deckService.findDeckAndVerifyOwnership(10L, "owner@example.com")).thenReturn(deck);
        when(cardRepository.countByDeckId(10L)).thenReturn(0L);

        Optional<CardResponse> result = cardService.getRandomCard(10L, "owner@example.com");

        assertThat(result).isEmpty();
    }
}
