# Self-Analysis — Lab 1

## Architecture

### 1. Where does the business logic live?

The business logic primarily resides in the **Service layer** (`DeckService`, `CardService`, `AuthService`). Controllers simply receive HTTP requests and delegate the work to the services. The services are responsible for validation (e.g., checking for duplicate deck titles), enforcing invariants (like ownership verification), and orchestrating database interactions via Repositories.

Some basic constraints are also expressed declaratively via Jakarta Validation annotations on the request DTOs (e.g., `@NotBlank`, `@Size`, `@Email`, `@Pattern`), which means the presentation layer participates in input validation before the service is even called.

### 2. How are DB models and business logic connected?

The application uses ORM models directly within the business logic. Entities like `Deck` and `User` are mapped directly to the database using JPA/Hibernate annotations (`@Entity`, `@Table`, `@ManyToOne`). The Service layer uses these exact ORM entities to apply business rules, modify state, and persist data. DTOs (like `CreateDeckRequest` or `DeckResponse`) are used in Controllers to separate the external API contract from the internal DB models.

### 3. How easy would it be to replace the database?

Replacing PostgreSQL with a NoSQL database like MongoDB would require significant effort because the domain models and persistence logic are tightly coupled through Spring Data JPA. You would need to change multiple files:
- Replace JPA annotations (`@Entity`, `@Column`, `@Id`) in all Entity classes with MongoDB equivalents (`@Document`, `@Field`).
- Update all Repository interfaces (`DeckRepository`, etc.) to extend `MongoRepository` instead of `JpaRepository`.
- Change dependencies in `pom.xml`/`build.gradle`.
- Review and potentially rewrite transactional logic (`@Transactional`), as MongoDB handles transactions differently than relational databases.
- The service layer would likely need adjustments too since it relies on JPA-specific behavior like cascade delete and unique constraints.
- `docker-compose.yml` and `application.yml` would also need to be reconfigured

Roughly 15-20 files would need modification.

### 4. How hard was it to test?

Testing the business logic is relatively easy and completely independent of the database infrastructure. The unit tests are written using Mockito without spinning up the Spring context or a real database. By mocking the `DeckRepository` and `UserRepository`, the tests execute instantly and verify the business rules in total isolation. 

The integration tests require Docker (for Testcontainers), which adds startup time (~5-10 seconds for PostgreSQL). Without Docker, integration tests cannot run at all.

## Scalability

### 5. How easy would it be to scale this service?

Scaling the service horizontally would be very easy from an application standpoint because the code is stateless. Authentication is handled via JWT, meaning there are no in-memory user sessions or global variables preventing you from running multiple application instances behind a load balancer.
However, if the load increased 100x, the architecture would need adjustments:

**What would break first** 

The relational database PostgreSQL would become the primary bottleneck. Connection pool exhaustion and heavy concurrent read/write locks would slow down the system.

**Architectural changes needed** 
  1. Introduce caching (e.g., Redis) for frequently accessed data (like public decks or user profiles).
  2. Implement database read replicas to separate and distribute the read load from the write load.
  3. Move towards a CQRS architecture to independently scale read and write operations.
