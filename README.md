# BookLibre — Backend

BookLibre is a community book-lending application. This repository contains the backend API that supports the web client: it manages member accounts, book listings, reservations, ratings, activity, and administrative metrics.

The backend is a Kotlin and Spring Boot application. It exposes REST endpoints for the core application workflows and GraphQL queries for administrative analytics. The project was developed as part of a group academic project.

The companion web client is maintained in the [BookLibre frontend repository](https://github.com/SRodriguezSpina/booklibre-frontend/tree/feat/ajustes-deploy).

## Architecture

```text
Web client
    │
    ├── REST /api/**            ── Spring MVC controllers
    └── GraphQL /api/graphql    ── Netflix DGS data fetchers
                                       │
                             Spring services and domain
                               ┌───────┼────────┐
                               │       │        │
                         PostgreSQL MongoDB   Redis
                         relational book     cache
                         data        documents
```

The application separates data by access pattern and responsibility:

| Component | Responsibility |
|---|---|
| **PostgreSQL** | Relational data such as users and reservations, plus relational history and read models. Spring Data JPA provides persistence; Flyway versions the schema and seed data. |
| **MongoDB** | Book catalogue documents and document-oriented book data. Spring Data MongoDB is used to query and update the catalogue. Local development uses a standalone MongoDB instance; the application does not require a self-managed sharded cluster or replica set. |
| **Redis** | Cache for frequently requested rankings, including popular books and top Bibliokarma users. Cache data is derived and can be rebuilt from persistent data. |
| **Spring services** | Business rules and coordination across repositories and data stores. Controllers and GraphQL fetchers delegate application behavior to these services. |

GraphQL analytics can combine information from more than one store through the service layer. The API currently exposes `tasaConversion`, `analisisCalificaciones`, `saludCatalogo`, `topBibliokarmas`, and `feedActividad`.

## Technology

- **Language:** Kotlin
- **Runtime:** Java 21
- **Framework:** Spring Boot 3
- **HTTP API:** Spring MVC, request validation, and Spring Security
- **GraphQL:** Netflix DGS
- **Persistence:** Spring Data JPA, Spring Data MongoDB, and Spring Data Redis
- **Relational migrations:** Flyway
- **API documentation:** Springdoc OpenAPI / Swagger UI
- **Tests:** JUnit 5, Kotest, MockK, Mockito, and Spring Boot Test
- **Containers:** Docker and Docker Compose

## API overview

The API is rooted at `/api`.

| Area | Example routes |
|---|---|
| Authentication | `POST /api/login`, `POST /api/register`, `POST /api/refresh`, `POST /api/logout` |
| Books | `GET /api/libros/home`, `GET /api/libros/{id}`, `POST /api/libros/nuevo`, `PUT /api/libros/{id}` |
| Reservations | `GET /api/reservas`, `POST /api/reservas/crear` |
| Members | `GET /api/usuarios/{id}`, `PUT /api/usuarios/{id}`, `GET /api/usuarios/{id}/libros` |
| GraphQL | `POST /api/graphql` |

The exact request parameters and response schemas are available in Swagger UI and in the controllers and GraphQL schema.

### Authentication and authorization

Login and registration issue short-lived access and refresh tokens in `HttpOnly` cookies. The web client sends these cookies with authenticated requests. Spring Security enforces access by account role; administrative REST and GraphQL queries require the admin role.

## Run locally

### Requirements

- Docker Compose
- Java 21 if running the backend directly on the host

### Run the complete local stack

From this repository:

```powershell
docker compose up --build
```

This starts the backend together with PostgreSQL, standalone MongoDB, and Redis. The API is available at `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI document: `http://localhost:8080/api-docs`
- GraphiQL: `http://localhost:8080/api/graphiql`

To stop the stack:

```powershell
docker compose down
```

PostgreSQL and MongoDB data are stored in Docker volumes. Redis contains cache data and may be rebuilt.

### Run the backend from the host

Start the data services first:

```powershell
docker compose up -d postgres mongo redis
```

Then run Spring Boot:

```powershell
.\gradlew.bat bootRun
```

The default Spring profile is `local`. It connects to the services on `localhost`; set `MONGODB_URI` to use a different local MongoDB connection string.

## Database migrations and seed data

Flyway applies versioned migrations from `src/main/resources/db/migration` and Kotlin migrations from `src/main/kotlin/db/migration` during startup. The migrations create the relational schema, seed initial users and reservations, initialize catalogue data in MongoDB, and apply later schema updates.

Migrations run automatically against a new database. Editing an already-applied migration does not rerun it; add a new versioned migration for subsequent schema changes.

## Tests

Run the backend test suite from this repository:

```powershell
.\gradlew.bat test
```

The test task also generates a JaCoCo coverage report under `build/reports` and `build/jacocoHtml`.

## Deployment

The project includes Docker and environment-specific Spring configuration for local, Docker, and production profiles. The current deployment design and required environment variables are documented in [`docs/deploy.md`](docs/deploy.md); the Render infrastructure blueprint is in [`render.yaml`](render.yaml).

Production credentials and connection strings should be provided through the hosting provider's secret/environment configuration, never committed to the repository. Review the documented limitations of free hosting plans before relying on them for persistent demo data.
