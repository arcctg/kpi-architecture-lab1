package com.flashcard.integration;

import com.flashcard.dto.request.CreateDeckRequest;
import com.flashcard.dto.request.RegisterRequest;
import com.flashcard.dto.request.UpdateDeckRequest;
import com.flashcard.dto.response.AuthResponse;
import com.flashcard.dto.response.DeckResponse;
import com.flashcard.repository.DeckRepository;
import com.flashcard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class DeckControllerIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    private String token;

    @BeforeEach
    void setUp() {
        deckRepository.deleteAll();
        userRepository.deleteAll();

        var reg = new RegisterRequest("deck@example.com", "Deck User", "password1");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity(
                "/api/auth/register", reg, AuthResponse.class);
        token = auth.getBody().token();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void createDeck_returns201() {
        var request = new CreateDeckRequest("Java Basics", "Core Java");

        ResponseEntity<DeckResponse> response = restTemplate.exchange(
                "/api/decks", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), DeckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().title()).isEqualTo("Java Basics");
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    void createDeck_duplicateTitle_returns409() {
        var request = new CreateDeckRequest("Duplicate", "First");
        restTemplate.exchange("/api/decks", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), DeckResponse.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/decks", HttpMethod.POST,
                new HttpEntity<>(new CreateDeckRequest("Duplicate", "Second"), authHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void listDecks_returnsPaginated() {
        restTemplate.exchange("/api/decks", HttpMethod.POST,
                new HttpEntity<>(new CreateDeckRequest("Deck 1", "d1"), authHeaders()),
                DeckResponse.class);
        restTemplate.exchange("/api/decks", HttpMethod.POST,
                new HttpEntity<>(new CreateDeckRequest("Deck 2", "d2"), authHeaders()),
                DeckResponse.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/decks?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Integer) response.getBody().get("totalElements")).isEqualTo(2);
    }

    @Test
    void getDeck_returns200() {
        ResponseEntity<DeckResponse> created = restTemplate.exchange(
                "/api/decks", HttpMethod.POST,
                new HttpEntity<>(new CreateDeckRequest("Get Test", "desc"), authHeaders()),
                DeckResponse.class);
        Long deckId = created.getBody().id();

        ResponseEntity<DeckResponse> response = restTemplate.exchange(
                "/api/decks/" + deckId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), DeckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().title()).isEqualTo("Get Test");
    }

    @Test
    void getDeck_notFound_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/decks/99999", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getDeck_otherUser_returns403() {
        ResponseEntity<DeckResponse> created = restTemplate.exchange(
                "/api/decks", HttpMethod.POST,
                new HttpEntity<>(new CreateDeckRequest("Private", "mine"), authHeaders()),
                DeckResponse.class);
        Long deckId = created.getBody().id();

        var otherReg = new RegisterRequest("other@example.com", "Other", "password1");
        ResponseEntity<AuthResponse> otherAuth = restTemplate.postForEntity(
                "/api/auth/register", otherReg, AuthResponse.class);
        HttpHeaders otherHeaders = new HttpHeaders();
        otherHeaders.setBearerAuth(otherAuth.getBody().token());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/decks/" + deckId, HttpMethod.GET,
                new HttpEntity<>(otherHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateDeck_returns200() {
        ResponseEntity<DeckResponse> created = restTemplate.exchange(
                "/api/decks", HttpMethod.POST,
                new HttpEntity<>(new CreateDeckRequest("Original", "desc"), authHeaders()),
                DeckResponse.class);
        Long deckId = created.getBody().id();

        var update = new UpdateDeckRequest("Updated", "new desc");
        ResponseEntity<DeckResponse> response = restTemplate.exchange(
                "/api/decks/" + deckId, HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders()), DeckResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().title()).isEqualTo("Updated");
    }

    @Test
    void deleteDeck_returns204() {
        ResponseEntity<DeckResponse> created = restTemplate.exchange(
                "/api/decks", HttpMethod.POST,
                new HttpEntity<>(new CreateDeckRequest("ToDelete", "d"), authHeaders()),
                DeckResponse.class);
        Long deckId = created.getBody().id();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/decks/" + deckId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/decks/" + deckId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
