package com.flashcard.repository;

import com.flashcard.entity.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    Page<Deck> findByOwnerId(Long ownerId, Pageable pageable);

    boolean existsByTitleAndOwnerId(String title, Long ownerId);
}
