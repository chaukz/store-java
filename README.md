# Store — E-Commerce Backend API

A RESTful e-commerce backend built with Spring Boot, modeling the core operations of an online store: browsing a product catalog, managing a cart, checking out, tracking orders, and handling payments.

This project is being built progressively, one vertical slice at a time — each feature (entity → repository → DTO → mapper → service → controller → error handling) is fully wired and tested before the next one is started.

## The Problem This Solves

Most e-commerce platforms share the same underlying domain, regardless of what's actually being sold: users need accounts and addresses, products need categories and variants (size, color, etc.), carts need to hold items before checkout, and orders need to track payment and fulfillment status independently of each other.

This project builds that domain from scratch as a standalone backend API — no framework-specific storefront, no vendor lock-in — so it can sit behind any frontend (a React app, a mobile client, or another service entirely) and expose clean, predictable REST endpoints for every part of that flow.

It's also a deliberate exercise in **layered architecture done properly**: entities never leak directly through the API, every error returns a consistent JSON shape instead of a stack trace, and business logic lives in one place (the service layer) rather than being scattered across controllers.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Connection Pooling | HikariCP |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Build Tool | Maven |
| Boilerplate Reduction | Lombok |
| Monitoring | Spring Boot Actuator |
| IDE | IntelliJ IDEA |
| OS (dev environment) | NixOS |

## Architecture

The codebase follows a strict layered structure — each layer has exactly one responsibility:

```
com.chaukz.store
├── controller   → receives HTTP requests, delegates to services, returns DTOs
├── service      → business logic; the only layer allowed to make decisions
├── repository   → Spring Data JPA interfaces; pure data access, no logic
├── model        → JPA entities mapped directly to database tables
│   └── enums    → Role, OrderStatus, PaymentStatus, PaymentMethod
├── dto
│   ├── request  → shape of data accepted from clients (with validation rules)
│   └── response → shape of data returned to clients (never the raw entity)
├── mapper       → converts between entities and DTOs in both directions
├── exception    → custom exceptions + a global handler for consistent error JSON
└── config       → application-level configuration
```

**Request flow, end to end:**

```
Client → Controller → Service → Mapper ↔ Repository → PostgreSQL
                          ↓
                    (business rules,
                     validation,
                     orchestration)
```

Controllers stay thin (routing only), services own the logic, mappers handle translation, and repositories never see a DTO.

## Database Schema

The schema is relational and models a standard e-commerce structure:

- **users** — accounts with role-based access (customer/admin)
- **addresses** — one-to-many with users, used for order shipping
- **categories** — product groupings
- **products** — belongs to a category; holds base info (name, brand, SKU, price)
- **product_variants** — belongs to a product; holds size/color-specific price and stock
- **carts** / **cart_items** — a user's in-progress selections before checkout
- **orders** / **order_items** — a finalized, historical snapshot of a purchase
- **payments** — tracks payment status and method against an order

## Current Progress

- [x] Domain modeling — all 10 JPA entities mapped to the schema
- [x] Repository layer — Spring Data JPA interfaces with custom finder methods
- [x] Global exception handling — standardized error responses (404, 400, 409, 500)
- [x] Category CRUD — full public + admin endpoints
- [x] Product CRUD — includes category lookup, filtering, and search
- [x] Product Variant CRUD — size/color variants scoped to a product
- [x] User registration + address management
- [x] Cart (add/update/remove items, stock validation)
- [x] Checkout → order creation
- [x] Payment status handling
- [x] Admin order fulfillment management
- [x] Authentication & authorization (Spring Security + JWT)
- [x] Pagination, filtering, and API documentation (OpenAPI/Swagger)
- [ ] Unit and integration tests

## API Overview (implemented so far)

**Public**
```
GET  /api/categories
GET  /api/categories/{id}
GET  /api/products
GET  /api/products?categoryId={id}
GET  /api/products/search?query={term}
GET  /api/products/{id}
GET  /api/products/{productId}/variants
```

**Admin**
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
```

All error responses follow a consistent shape:
```json
{
  "message": "Product not found with id: 999",
  "status": 404,
  "timestamp": "2026-07-26T18:45:00"
}
```

## Running Locally

**Prerequisites:** Java 21, Maven, PostgreSQL running locally.

1. Create a database named `storeDB` in PostgreSQL.
2. Configure your credentials in `src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/storeDB
       username: <your_username>
       password: <your_password>
     jpa:
       hibernate:
         ddl-auto: update
       show-sql: true
   ```
3. Run the app:
   ```bash
   mvn spring-boot:run
   ```
4. The app starts on `http://localhost:8081` (or whatever port is configured).

## Author

Built by [chaukz](https://github.com/chaukz)
