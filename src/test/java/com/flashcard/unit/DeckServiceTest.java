package com.flashcard.unit;

import com.flashcard.dto.request.CreateDeckRequest;
import com.flashcard.dto.request.UpdateDeckRequest;
import com.flashcard.dto.response.DeckResponse;
import com.flashcard.entity.Deck;
import com.flashcard.entity.User;
import com.flashcard.exception.DuplicateResourceException;
import com.flashcard.exception.ForbiddenAccessException;
import com.flashcard.exception.ResourceNotFoundException;
import com.flashcard.repository.DeckRepository;
import com.flashcard.repository.UserRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeckService deckService;

    private User owner;
    private Deck deck;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        deck = new Deck();
        deck.setId(10L);
        deck.setTitle("Java Basics");
        deck.setDescription("Core Java terms");
        deck.setOwner(owner);
        deck.setCards(new ArrayList<>());
    }

    @Test
    void listDecks_returnsPage() {
        var pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(deckRepository.findByOwnerId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(deck)));

        Page<DeckResponse> result = deckService.listDecks("owner@example.com", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Java Basics");
    }

    @Test
    void createDeck_success() {
        var request = new CreateDeckRequest("Spring Boot", "Spring terms");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(deckRepository.existsByTitleAndOwnerId("Spring Boot", 1L)).thenReturn(false);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> {
            Deck saved = inv.getArgument(0);
            saved.setId(20L);
            saved.setCards(new ArrayList<>());
            return saved;
        });

        DeckResponse response = deckService.createDeck(request, "owner@example.com");

        assertThat(response.title()).isEqualTo("Spring Boot");
        verify(deckRepository).save(any(Deck.class));
    }

    @Test
    void createDeck_duplicateTitle_throws409() {
        var request = new CreateDeckRequest("Java Basics", "Duplicate");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(deckRepository.existsByTitleAndOwnerId("Java Basics", 1L)).thenReturn(true);

        assertThatThrownBy(() -> deckService.createDeck(request, "owner@example.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Java Basics");
    }

    @Test
    void getDeck_notFound_throws404() {
        when(deckRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.getDeck(999L, "owner@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDeck_notOwner_throws403() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));

        assertThatThrownBy(() -> deckService.getDeck(10L, "other@example.com"))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void updateDeck_success() {
        var request = new UpdateDeckRequest("Updated Title", "Updated desc");
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(deckRepository.existsByTitleAndOwnerId("Updated Title", 1L)).thenReturn(false);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));

        DeckResponse response = deckService.updateDeck(10L, request, "owner@example.com");

        assertThat(response.title()).isEqualTo("Updated Title");
    }

    @Test
    void updateDeck_duplicateTitle_throws409() {
        var request = new UpdateDeckRequest("Existing Title", "desc");
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(deckRepository.existsByTitleAndOwnerId("Existing Title", 1L)).thenReturn(true);

        assertThatThrownBy(() -> deckService.updateDeck(10L, request, "owner@example.com"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteDeck_success() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));

        deckService.deleteDeck(10L, "owner@example.com");

        verify(deckRepository).delete(deck);
    }
}
