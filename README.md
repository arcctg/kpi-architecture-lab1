# Flashcard Dictionary API

A REST API for managing flashcard decks and cards, built with Java 21 and Spring Boot 3.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for PostgreSQL and integration tests)

## Getting Started

### 1. Start the database

```bash
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` with database `flashcard`.

### 2. Build the project

```bash
mvn clean install -DskipTests
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

## Running Tests

### Unit tests only

```bash
mvn test
```

### All tests (unit + integration)

Integration tests use Testcontainers and require Docker to be running.

```bash
mvn verify
```

## API Overview

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT |

### Decks

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/decks` | List your decks (paginated) |
| POST | `/api/decks` | Create a new deck |
| GET | `/api/decks/{id}` | Get deck details |
| PUT | `/api/decks/{id}` | Update a deck |
| DELETE | `/api/decks/{id}` | Delete a deck (cascades to cards) |

### Cards

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/decks/{deckId}/cards?search=` | List cards (paginated, filterable) |
| GET | `/api/decks/{deckId}/cards/random` | Get a random card from deck |
| POST | `/api/decks/{deckId}/cards` | Add a card |
| GET | `/api/decks/{deckId}/cards/{cardId}` | Get card details |
| PUT | `/api/decks/{deckId}/cards/{cardId}` | Update a card |
| DELETE | `/api/decks/{deckId}/cards/{cardId}` | Delete a card |

All endpoints except `/api/auth/**` require `Authorization: Bearer <token>` header.

## Swagger UI

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

1. Start the application
2. Open the Swagger UI URL in your browser
3. Click **Authorize** and enter your JWT token (obtained from `/api/auth/login` or `/api/auth/register`)
4. Test any endpoint directly from the UI

## Usage Example

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","displayName":"User","password":"password1"}'

# Create a deck (use token from register response)
curl -X POST http://localhost:8080/api/decks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"Java Basics","description":"Core Java terms"}'

# Add a card
curl -X POST http://localhost:8080/api/decks/1/cards \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"term":"JVM","definition":"Java Virtual Machine"}'
```

## Project Structure

```
src/main/java/com/flashcard/
├── config/          # Security configuration
├── controller/      # REST controllers
├── dto/             # Request/response DTOs
├── entity/          # JPA entities
├── exception/       # Exception handling
├── repository/      # Spring Data repositories
├── security/        # JWT authentication
└── service/         # Business logic
```
