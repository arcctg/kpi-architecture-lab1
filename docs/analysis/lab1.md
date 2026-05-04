# Self-Analysis — Lab 1

## Architecture

### 1. Where does the business logic live?

The business logic in this project primarily resides in the **service layer** (`AuthService`, `DeckService`, `CardService`). All validation, invariant checks (email uniqueness, deck title uniqueness per owner, card term uniqueness within a deck), and ownership verification happen in these service classes. Some basic constraints are also expressed declaratively via Jakarta Validation annotations on the request DTOs (e.g., `@NotBlank`, `@Size`, `@Email`, `@Pattern`), which means the presentation layer participates in input validation before the service is even called.

The JPA entities themselves are mostly passive data holders — they define the database schema through annotations and lifecycle callbacks (`@PrePersist`, `@PreUpdate`), but do not contain any real business rules. This is a classic **anemic model** approach: the entities carry data, and the services carry behavior.

### 2. How are DB models and business logic connected?

The JPA entities (`User`, `Deck`, `Card`) are used **directly** throughout the entire application — from the repository layer all the way to the service layer. There is no separation between a "domain model" and a "persistence model"; they are the same classes. The service layer reads and writes these entities directly, and the response DTOs (`DeckResponse`, `CardResponse`) are created from the JPA entities using static factory methods (`DeckResponse.from(deck)`).

This tight coupling means that any change to the database schema (e.g., renaming a column, splitting a table) would require changes across the services and DTOs as well. The ORM annotations (`@Entity`, `@Table`, `@Column`) are interleaved with the data the business logic depends on.

### 3. How easy would it be to replace the database?

Replacing PostgreSQL with, say, MongoDB would require significant changes. Specifically, every JPA entity would need to be rewritten (JPA annotations removed, Mongo annotations added), every repository interface would change (from `JpaRepository` to `MongoRepository`), and the service layer would likely need adjustments too since it relies on JPA-specific behavior like cascade delete and unique constraints. The `docker-compose.yml` and `application.yml` would also need to be reconfigured. Roughly **15-20 files** would need modification.

Spring Data's abstraction helps somewhat — the repository method signatures (like `findByOwnerId`) would remain similar — but the entity model, the constraints, and the infrastructure configuration are all tightly bound to JPA/PostgreSQL.

### 4. How hard was it to test?

The **unit tests** were relatively straightforward because the services accept their dependencies (repositories) via constructor injection, which makes them easy to mock with Mockito. The unit tests run without a database or Spring context, and they execute quickly.

The **integration tests** require Docker (for Testcontainers), which adds startup time (~5-10 seconds for PostgreSQL). Without Docker, integration tests cannot run at all. The unit tests, however, do not depend on the database or Spring framework at all — they test pure service logic with mocked repositories.

One downside is that the service layer is somewhat hard to test in isolation for invariants that involve database lookups (like uniqueness checks), because the check logic and the persistence logic are in the same method. This makes it necessary to carefully set up mocks for methods like `existsByTitleAndOwnerId`.

## Scalability

### 5. How easy would it be to scale this service?

Under a 100x load increase, several pain points would emerge:

- **Database bottleneck**: All requests hit a single PostgreSQL instance. The lack of caching (no Redis, no in-memory cache) means every read goes to the database. Adding read replicas and a caching layer would be the first step.
- **Stateless design**: The JWT-based authentication is stateless, and there are no in-memory caches or global variables, which is good. Multiple application instances can run behind a load balancer without session affinity issues.
- **Unique constraint checks**: The pattern of `existsByX` followed by `save` is not atomic and could suffer from race conditions under concurrent writes (two requests could both pass the uniqueness check and then one would fail at the database constraint level). This is handled by the database's unique constraint, but the error mapping from a `DataIntegrityViolationException` to a 409 is not implemented — this would need to be added.
- **Pagination**: Pagination is built in, which prevents unbounded result sets.

Architectural changes for scale: introduce a caching layer, consider database connection pooling tuning (HikariCP defaults may not be sufficient), and add proper race condition handling for uniqueness checks.
