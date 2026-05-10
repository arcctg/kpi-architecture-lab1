package com.flashcard.integration;

import com.flashcard.dto.request.CreateCardRequest;
import com.flashcard.dto.request.CreateDeckRequest;
import com.flashcard.dto.request.RegisterRequest;
import com.flashcard.dto.request.UpdateCardRequest;
import com.flashcard.dto.response.AuthResponse;
import com.flashcard.dto.response.CardResponse;
import com.flashcard.dto.response.DeckResponse;
import com.flashcard.repository.CardRepository;
import com.flashcard.repository.DeckRepository;
import com.flashcard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CardControllerIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private CardRepository cardRepository;

    private String token;
    private Long deckId;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        deckRepository.deleteAll();
        userRepository.deleteAll();

        var reg = new RegisterRequest("card@example.com", "Card User", "password1");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity(
                "/api/auth/register", reg, AuthResponse.class);
        token = auth.getBody().token();

        ResponseEntity<DeckResponse> deck = restTemplate.exchange(
                "/api/decks", HttpMethod.POST,
                new HttpEntity<>(new CreateDeckRequest("Test Deck", "for cards"), authHeaders()),
                DeckResponse.class);
        deckId = deck.getBody().id();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String cardsUrl() {
        return "/api/decks/" + deckId + "/cards";
    }

    @Test
    void createCard_returns201() {
        var request = new CreateCardRequest("JVM", "Java Virtual Machine");

        ResponseEntity<CardResponse> response = restTemplate.exchange(
                cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), CardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().term()).isEqualTo("JVM");
        assertThat(response.getBody().deckId()).isEqualTo(deckId);
    }

    @Test
    void createCard_duplicateTerm_returns409() {
        var request = new CreateCardRequest("GC", "Garbage Collector");
        restTemplate.exchange(cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), CardResponse.class);

        ResponseEntity<String> response = restTemplate.exchange(
                cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("GC", "Another def"), authHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void listCards_returnsPaginated() {
        restTemplate.exchange(cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("Term1", "Def1"), authHeaders()),
                CardResponse.class);
        restTemplate.exchange(cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("Term2", "Def2"), authHeaders()),
                CardResponse.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                cardsUrl() + "?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Integer) response.getBody().get("totalElements")).isEqualTo(2);
    }

    @Test
    void listCards_withSearch_filtersResults() {
        restTemplate.exchange(cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("Java", "Language"), authHeaders()),
                CardResponse.class);
        restTemplate.exchange(cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("Python", "Language"), authHeaders()),
                CardResponse.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                cardsUrl() + "?search=jav", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Integer) response.getBody().get("totalElements")).isEqualTo(1);
    }

    @Test
    void getCard_returns200() {
        ResponseEntity<CardResponse> created = restTemplate.exchange(
                cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("API", "Application Programming Interface"),
                        authHeaders()),
                CardResponse.class);
        Long cardId = created.getBody().id();

        ResponseEntity<CardResponse> response = restTemplate.exchange(
                cardsUrl() + "/" + cardId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), CardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().term()).isEqualTo("API");
    }

    @Test
    void updateCard_returns200() {
        ResponseEntity<CardResponse> created = restTemplate.exchange(
                cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("Old", "Old def"), authHeaders()),
                CardResponse.class);
        Long cardId = created.getBody().id();

        var update = new UpdateCardRequest("New", "New def");
        ResponseEntity<CardResponse> response = restTemplate.exchange(
                cardsUrl() + "/" + cardId, HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders()), CardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().term()).isEqualTo("New");
    }

    @Test
    void deleteCard_returns204() {
        ResponseEntity<CardResponse> created = restTemplate.exchange(
                cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("ToDelete", "bye"), authHeaders()),
                CardResponse.class);
        Long cardId = created.getBody().id();

        ResponseEntity<Void> response = restTemplate.exchange(
                cardsUrl() + "/" + cardId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getRandomCard_returns200() {
        restTemplate.exchange(cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("Term1", "Def1"), authHeaders()),
                CardResponse.class);
        restTemplate.exchange(cardsUrl(), HttpMethod.POST,
                new HttpEntity<>(new CreateCardRequest("Term2", "Def2"), authHeaders()),
                CardResponse.class);

        ResponseEntity<CardResponse> response = restTemplate.exchange(
                cardsUrl() + "/random", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), CardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().term()).isIn("Term1", "Term2");
        assertThat(response.getBody().deckId()).isEqualTo(deckId);
    }

    @Test
    void getRandomCard_emptyDeck_returns204() {
        ResponseEntity<Void> response = restTemplate.exchange(
                cardsUrl() + "/random", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getRandomCard_unauthorized_returns401() {
        ResponseEntity<String> response = restTemplate.exchange(
                cardsUrl() + "/random", HttpMethod.GET,
                HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

