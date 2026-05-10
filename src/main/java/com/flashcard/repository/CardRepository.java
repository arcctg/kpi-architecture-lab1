package com.flashcard.repository;

import com.flashcard.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByDeckId(Long deckId, Pageable pageable);

    Page<Card> findByDeckIdAndTermContainingIgnoreCase(Long deckId, String term, Pageable pageable);

    boolean existsByTermAndDeckId(String term, Long deckId);

    long countByDeckId(Long deckId);
}
