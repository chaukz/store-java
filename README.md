# Store — E-Commerce Backend API

A RESTful e-commerce backend built with Spring Boot. Handles the full customer journey: browsing a product catalog, managing a cart, checking out, tracking orders, and managing payments — all behind JWT-secured endpoints with role-based access control.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 |
| Persistence | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Database | PostgreSQL |
| Security | Spring Security + JWT (jjwt 0.12) |
| Validation | Jakarta Bean Validation |
| Build | Maven |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI (Swagger UI) |

## Architecture

Strict layered structure — each layer has one job:

```
Controller → Service → Repository → PostgreSQL
                 ↕
           Mapper ↔ DTO (request / response)
```

- **Controllers** — routing only, no business logic, entities never exposed
- **Services** — all decisions made here; the only layer that can talk to multiple repositories
- **Repositories** — Spring Data JPA interfaces, no logic
- **Mappers** — entity ↔ DTO translation, no logic
- **DTOs** — the API surface; requests carry validation annotations, responses are what clients see
- **`exception/`** — custom exceptions + `GlobalExceptionHandler` that returns a uniform error shape for every failure

Error responses always look like:
```json
{
  "message": "Product not found with id: 42",
  "status": 404,
  "timestamp": "2026-09-16T10:30:00"
}
```

## Domain Model

```
categories ──< products ──< product_variants
                                  │
users ──< addresses               │
  │                               │
  └──< carts ──< cart_items ──────┤
  │                               │
  └──< orders ──< order_items ────┘
          │
          └── payments (1:1)
```

Key design decisions:
- **Variants carry price and stock** — `product_variants` holds `price` and `stock_quantity`, not the parent product. A product is just a grouping; you always buy a specific variant (size/color).
- **Order items snapshot product info** — at checkout, `order_items` captures `product_name` and `product_sku` so historical orders remain accurate even if a variant is later deleted.
- **Optimistic locking on variants** — `product_variants` has a `@Version` column. Concurrent checkouts of the same variant will race; only one wins, the other gets a 409 instead of silently overselling.
- **One cart per user** — created automatically on registration, cleared after a successful checkout.

## Security

- **Stateless JWT** — every protected request must carry `Authorization: Bearer <token>`.
- **Public routes** — `GET /api/products/**`, `GET /api/categories/**`, `/api/auth/**`, Swagger UI.
- **Admin routes** — everything under `/api/admin/**` requires `ROLE_ADMIN`.
- **Authenticated routes** — cart, orders, addresses, user profile (`/api/users/me`).
- **User endpoints are locked to self** — `/api/users/me` resolves the user from the JWT, never from a path param. Regular users cannot read or edit other accounts.
- **Seeded admin** — on first boot, `AdminSeeder` creates `admin@store.com` / `admin123` if no admin exists yet (breaks the chicken-and-egg: creating a user requires ADMIN, but there's no ADMIN until a user exists).

## Database Migrations

Schema is managed by Flyway (`src/main/resources/db/migration/`). `ddl-auto=validate` — Hibernate never modifies the schema. To change the schema, add a new `V{n+1}__description.sql` file; never edit existing migrations.

| Migration | Description |
|---|---|
| `V1` | Full initial schema — all tables and indexes |
| `V2` | Adds `version` column to `product_variants` for optimistic locking |
| `V3` | Drops `price` and `stock_quantity` from `products` (moved to variants) |
| `V4` | Adds `product_name` and `product_sku` snapshot columns to `order_items`; relaxes the FK to `ON DELETE SET NULL` |

## API Reference

### Auth
```
POST /api/auth/register     — create a CUSTOMER account, returns JWT
POST /api/auth/login        — authenticate, returns JWT
```

### Catalog (public)
```
GET /api/categories
GET /api/categories/{id}
GET /api/products                         — supports ?categoryId=, ?page=, ?size=
GET /api/products/search?query={term}
GET /api/products/{id}
GET /api/products/{productId}/variants
```

### Cart (authenticated)
```
GET    /api/cart
POST   /api/cart/items
PUT    /api/cart/items/{cartItemId}
DELETE /api/cart/items/{cartItemId}
DELETE /api/cart
```

### Orders (authenticated)
```
POST /api/checkout
GET  /api/orders
GET  /api/orders/{orderId}
POST /api/orders/{orderId}/cancel
GET  /api/orders/{orderId}/payment
```

Order lifecycle: `PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED` (or `CANCELLED` from any non-terminal state). Invalid transitions return 400.

Payment status: `PENDING → PAID → REFUNDED | FAILED`.

### Addresses (authenticated)
```
GET    /api/users/me/addresses
POST   /api/users/me/addresses
PUT    /api/users/me/addresses/{id}
DELETE /api/users/me/addresses/{id}
```

### User profile (authenticated)
```
GET /api/users/me
PUT /api/users/me
```

### Admin
```
POST   /api/admin/categories
PUT    /api/admin/categories/{id}
DELETE /api/admin/categories/{id}

POST   /api/admin/products
PUT    /api/admin/products/{id}
DELETE /api/admin/products/{id}

POST   /api/admin/products/{productId}/variants
PUT    /api/admin/variants/{variantId}
DELETE /api/admin/variants/{variantId}

GET    /api/admin/orders             — paginated, filterable by status
PUT    /api/admin/orders/{id}/status — enforce fulfillment state machine

GET    /api/admin/payments           — paginated, filterable by status
PUT    /api/admin/payments/{id}/status

GET    /api/admin/users
GET    /api/admin/users/{id}
POST   /api/admin/users
PUT    /api/admin/users/{id}
DELETE /api/admin/users/{id}
```

## Running Locally

**Prerequisites:** Java 21, Maven, PostgreSQL.

### Option A — Docker (recommended)

```bash
cp .env.example .env   # fill in values
docker compose up -d   # starts Postgres on 5432 + Adminer on 8080
```

### Option B — local Postgres

Create a database named `storeDB`, then set the required environment variables.

### Required environment variables

| Variable | Description |
|---|---|
| `DB_PASSWORD` | Postgres password |
| `JWT_SECRET` | Secret key for signing JWTs (any long random string) |

Optional (defaults shown):

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/storeDB` |
| `DB_USERNAME` | `chaukz` |
| `SERVER_PORT` | `8081` |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` |

### Start the app

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8081`. Swagger UI is available at `http://localhost:8081/swagger-ui.html`.

## Tests

```bash
./mvnw test                            # all tests
./mvnw test -Dtest=OrderServiceTest    # single class
```

Service-layer tests use Mockito with mocked repositories. Controller tests use MockMvc. `StockOptimisticLockingTest` verifies that concurrent checkouts for the same variant correctly conflict rather than oversell.

## Author

Built by [chaukz](https://github.com/chaukz)
