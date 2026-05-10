# Use Cases — Flashcard Dictionary

## Actors

- **Guest** — unauthenticated user
- **Authenticated User** — a registered and logged-in user

---

## UC-1: Register

| Field | Description |
|---|---|
| **Actor** | Guest |
| **Preconditions** | None |
| **Main Scenario** | 1. Guest sends email, displayName, and password. 2. System validates: email format (RFC 5322), displayName 2-50 chars, password ≥ 8 chars with at least one letter and one digit. 3. System checks email uniqueness. 4. System creates user and returns a JWT token. |
| **Errors** | 400 — invalid email, short displayName, or weak password. 409 — email already registered. |

## UC-2: Login

| Field | Description |
|---|---|
| **Actor** | Guest |
| **Preconditions** | User account exists |
| **Main Scenario** | 1. Guest sends email and password. 2. System verifies credentials. 3. System returns a JWT token. |
| **Errors** | 401 — invalid email or wrong password. |

---

## UC-3: Create Deck

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | User is logged in |
| **Main Scenario** | 1. User sends title (1-100 chars) and optional description. 2. System checks title uniqueness within user's decks. 3. System creates deck and returns it with 201. |
| **Errors** | 400 — blank or too long title. 409 — deck with this title already exists for this user. 401 — not authenticated. |

## UC-4: List My Decks

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | User is logged in |
| **Main Scenario** | 1. User requests their decks with optional pagination (page, size). 2. System returns a paginated list of decks owned by the user, each with a card count. |
| **Errors** | 401 — not authenticated. |

## UC-5: Get Deck

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | Deck exists and is owned by the user |
| **Main Scenario** | 1. User requests a deck by ID. 2. System verifies ownership. 3. System returns the deck details with card count. |
| **Errors** | 404 — deck not found. 403 — user does not own the deck. 401 — not authenticated. |

## UC-6: Update Deck

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | Deck exists and is owned by the user |
| **Main Scenario** | 1. User sends new title and/or description. 2. System verifies ownership. 3. System checks new title uniqueness (if changed). 4. System updates and returns the deck. |
| **Errors** | 400 — invalid data. 404 — deck not found. 403 — not owner. 409 — duplicate title. 401 — not authenticated. |

## UC-7: Delete Deck

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | Deck exists and is owned by the user |
| **Main Scenario** | 1. User requests deletion by deck ID. 2. System verifies ownership. 3. System deletes the deck and all its cards (cascade). 4. Returns 204. |
| **Errors** | 404 — deck not found. 403 — not owner. 401 — not authenticated. |

---

## UC-8: Add Card to Deck

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | Deck exists and is owned by the user |
| **Main Scenario** | 1. User sends term (1-200 chars) and definition (1-500 chars). 2. System verifies deck ownership. 3. System checks term uniqueness within the deck. 4. System creates the card and returns it with 201. |
| **Errors** | 400 — blank/too long term or definition. 404 — deck not found. 403 — not owner. 409 — duplicate term in deck. 401 — not authenticated. |

## UC-9: List Cards in Deck

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | Deck exists and is owned by the user |
| **Main Scenario** | 1. User requests cards for a deck with optional pagination and optional `search` parameter. 2. System verifies deck ownership. 3. If `search` is provided, system filters cards by term (case-insensitive contains). 4. System returns a paginated list of cards. |
| **Errors** | 404 — deck not found. 403 — not owner. 401 — not authenticated. |

## UC-10: Get Card

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | Card exists in a deck owned by the user |
| **Main Scenario** | 1. User requests a card by deck ID and card ID. 2. System verifies deck ownership and card membership. 3. System returns card details. |
| **Errors** | 404 — deck or card not found (or card not in deck). 403 — not owner. 401 — not authenticated. |

## UC-11: Update Card

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | Card exists in a deck owned by the user |
| **Main Scenario** | 1. User sends new term and/or definition. 2. System verifies ownership. 3. System checks new term uniqueness within the deck (if changed). 4. System updates and returns the card. |
| **Errors** | 400 — invalid data. 404 — not found. 403 — not owner. 409 — duplicate term. 401 — not authenticated. |

## UC-12: Delete Card

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | Card exists in a deck owned by the user |
| **Main Scenario** | 1. User requests deletion by deck ID and card ID. 2. System verifies ownership and card membership. 3. System deletes the card. 4. Returns 204. |
| **Errors** | 404 — not found. 403 — not owner. 401 — not authenticated. |

## UC-13: Get Random Card from Deck

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | Deck exists and is owned by the user |
| **Main Scenario** | 1. User requests a random card from a deck by deck ID. 2. System verifies deck ownership. 3. System counts cards in the deck. 4. System selects a random card. 5. System returns the card details with 200. |
| **Alternative Scenario** | If the deck has no cards, the system returns 204 No Content. |
| **Errors** | 404 — deck not found. 403 — user does not own the deck. 401 — not authenticated. |
