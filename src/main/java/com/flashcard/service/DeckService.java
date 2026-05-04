package com.flashcard.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final UserRepository userRepository;

    public DeckService(DeckRepository deckRepository, UserRepository userRepository) {
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<DeckResponse> listDecks(String ownerEmail, Pageable pageable) {
        User owner = findUserByEmail(ownerEmail);
        return deckRepository.findByOwnerId(owner.getId(), pageable)
                .map(DeckResponse::from);
    }

    @Transactional(readOnly = true)
    public DeckResponse getDeck(Long deckId, String ownerEmail) {
        Deck deck = findDeckAndVerifyOwnership(deckId, ownerEmail);
        return DeckResponse.from(deck);
    }

    @Transactional
    public DeckResponse createDeck(CreateDeckRequest request, String ownerEmail) {
        User owner = findUserByEmail(ownerEmail);

        if (deckRepository.existsByTitleAndOwnerId(request.title(), owner.getId())) {
            throw new DuplicateResourceException(
                    "Deck with title '" + request.title() + "' already exists");
        }

        Deck deck = new Deck();
        deck.setTitle(request.title());
        deck.setDescription(request.description());
        deck.setOwner(owner);
        deck = deckRepository.save(deck);

        return DeckResponse.from(deck);
    }

    @Transactional
    public DeckResponse updateDeck(Long deckId, UpdateDeckRequest request, String ownerEmail) {
        Deck deck = findDeckAndVerifyOwnership(deckId, ownerEmail);

        if (!deck.getTitle().equals(request.title())
                && deckRepository.existsByTitleAndOwnerId(request.title(), deck.getOwner().getId())) {
            throw new DuplicateResourceException(
                    "Deck with title '" + request.title() + "' already exists");
        }

        deck.setTitle(request.title());
        deck.setDescription(request.description());
        deck = deckRepository.save(deck);

        return DeckResponse.from(deck);
    }

    @Transactional
    public void deleteDeck(Long deckId, String ownerEmail) {
        Deck deck = findDeckAndVerifyOwnership(deckId, ownerEmail);
        deckRepository.delete(deck);
    }

    public Deck findDeckAndVerifyOwnership(Long deckId, String ownerEmail) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found"));
        if (!deck.getOwner().getEmail().equals(ownerEmail)) {
            throw new ForbiddenAccessException("You do not own this deck");
        }
        return deck;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
