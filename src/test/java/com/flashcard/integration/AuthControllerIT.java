package com.flashcard.integration;

import com.flashcard.dto.request.LoginRequest;
import com.flashcard.dto.request.RegisterRequest;
import com.flashcard.dto.response.AuthResponse;
import com.flashcard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;

class AuthControllerIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_success_returns201() {
        var request = new RegisterRequest("test@example.com", "Test User", "password1");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void register_duplicateEmail_returns409() {
        var request = new RegisterRequest("dup@example.com", "User", "password1");
        restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_invalidEmail_returns400() {
        var request = new RegisterRequest("not-an-email", "User", "password1");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_weakPassword_returns400() {
        var request = new RegisterRequest("ok@example.com", "User", "nodigits");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_success_returns200() {
        var reg = new RegisterRequest("login@example.com", "User", "password1");
        restTemplate.postForEntity("/api/auth/register", reg, AuthResponse.class);

        var login = new LoginRequest("login@example.com", "password1");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() {
        var reg = new RegisterRequest("auth@example.com", "User", "password1");
        restTemplate.postForEntity("/api/auth/register", reg, AuthResponse.class);

        var login = new LoginRequest("auth@example.com", "wrongpass");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", login, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_noToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/decks", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withToken_returns200() {
        var reg = new RegisterRequest("protected@example.com", "User", "password1");
        ResponseEntity<AuthResponse> authResp = restTemplate.postForEntity(
                "/api/auth/register", reg, AuthResponse.class);
        String token = authResp.getBody().token();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/decks", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
