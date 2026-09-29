# Plazoleta Restaurants

Standalone Java 17 / Spring Boot 3.2.4 microservice for restaurant and dish management in the Plaza de Comidas challenge. It implements HU02, HU03, HU04, HU07, HU09 and the shared JWT validation needed for HU05. **HU10 onward are out of scope.**

## Implemented APIs

All application endpoints require `Authorization: Bearer <JWT>`.

| Method and path | Role | Purpose |
| --- | --- | --- |
| `POST /api/v1/restaurants` | `ROLE_ADMIN` | HU02: create restaurant |
| `GET /api/v1/restaurants?page=0&size=10` | `ROLE_CLIENTE` | HU09: alphabetic, paginated list for clients. Each item exposes only `name` and `logoUrl`. Page size is 1–100. |
| `POST /api/v1/restaurants/{restaurantId}/dishes` | `ROLE_PROPIETARIO` | HU03: create dish for a restaurant owned by the caller |
| `PATCH /api/v1/dishes/{dishId}` | `ROLE_PROPIETARIO` | HU04: update only `price` and `description`, and only for the caller's restaurant |
| `PATCH /api/v1/dishes/{dishId}/status` | `ROLE_PROPIETARIO` | HU07: set `{ "active": true/false }` for a dish in the caller's restaurant |
| `GET /internal/restaurantes/{restaurantId}/propietarios/{proprietorId}` | `ROLE_PROPIETARIO` | HU06 integration: returns JSON `true` only when the authenticated proprietor (`sub`) matches `proprietorId` and owns that restaurant; otherwise `false` |

Roles are enforced server-side. HU02 is administrator-only and confirms the submitted numeric proprietor ID against the user service while forwarding the authenticated admin token. HU03/HU04/HU07 require `ROLE_PROPIETARIO`; ownership comes from the validated JWT `sub`, never from a request body or query parameter, and each operation checks that the referenced restaurant belongs to that ID. HU09 is restricted to `ROLE_CLIENTE`.

Restaurant creation JSON requires `name`, numeric-string `nit`, `address`, numeric `phone` (optional leading `+`, at most 13 characters), `logoUrl`, and numeric-string `ownerId`. Names may include digits, but cannot be blank or all digits. A duplicate NIT returns `409`.

## HU checklist for this service

- [x] **HU02 - Create restaurant:** required fields and formats are validated; owner ID must be a positive numeric user ID; the user service confirms the `ROLE_PROPIETARIO`; the authenticated caller must be `ROLE_ADMIN`.
- [x] **HU03 - Create dish:** requires name, positive integer price, description, image URL and category; the restaurant owner is derived from JWT and the new dish starts active by default.
- [x] **HU04 - Modify dish:** only price and description can be changed; name, category, image, restaurant and active state are preserved.
- [x] **HU07 - Enable/disable dish:** only the owner of the dish's restaurant can change active state.
- [x] **HU09 - List restaurants:** only `ROLE_CLIENTE`; sorted alphabetically, paginated, with response limited to name and logo URL.
- [ ] **HU10 and later:** outside this implementation. In particular, listing/filtering dishes and all order processing are not included.

Dish creation requires `name`, positive integer `price`, `description`, `imageUrl` and `category`; `active` is optional and defaults to `true`. Invalid request fields return `400`; missing resources return `404`; ownership/role failures return `403`.

Swagger UI is at `/swagger-ui/index.html`; OpenAPI JSON is at `/v3/api-docs`. Both documentation paths are public.

## JWT and user-service integration

The sibling user service issues HS256 JWTs with `iss=plazoleta-usuarios`; both services must use the same `JWT_SECRET` (UTF-8 bytes), numeric `sub` containing the user ID, and `role` containing a value such as `ROLE_ADMIN`, `ROLE_PROPIETARIO` or `ROLE_CLIENTE`. This service validates the signature, expected issuer, timestamps, and numeric subject through Spring Security's JWT decoder and uses `sub` to check restaurant ownership. The secret must contain at least 32 characters; no built-in development secret is provided. Invalid, expired, wrong-issuer, or malformed-subject tokens are rejected; there is no open/fallback authorization.

HU02 checks the supplied numeric owner ID through the `UserValidationPort` by forwarding the administrator's original bearer token unchanged to the user service's protected `GET /internal/usuarios/{usuarioId}/roles/{rol}` with `rol=ROLE_PROPIETARIO`. This returns JSON `true` or `false`. Configure `USERS_ROLE_URL_TEMPLATE` with `{usuarioId}` and `{rol}` placeholders; its default is `http://localhost:8081/internal/usuarios/{usuarioId}/roles/{rol}`. The administrator token must be a trusted user-service JWT with the shared secret and expected issuer. A non-owner/missing user returns `400` from restaurant creation; an unavailable/rejected upstream response fails closed with `502`. No service credential or token is logged or reconstructed.

For HU06, the user service forwards the authenticated proprietor's bearer JWT to the protected ownership endpoint above. It requires `ROLE_PROPIETARIO` and confirms that the JWT subject matches the path proprietor ID before checking persisted ownership. It returns `200` with JSON `true` or `false`; a false result makes employee registration fail closed. This endpoint does not create employees or disclose restaurant details.

## Run and configuration

From this directory:

```powershell
$env:JWT_SECRET = 'replace-with-the-same-random-secret-of-at-least-32-characters'
.\mvnw.cmd spring-boot:run
```

For local integration, configure exactly the same `JWT_SECRET` for user service (port `8081`) and this service (port `8082`). Log in through `POST http://localhost:8081/auth/login` to obtain a user-service-issued JWT, then send it as `Authorization: Bearer <accessToken>` to restaurant endpoints. The restaurant service only trusts correctly signed HS256 tokens from issuer `plazoleta-usuarios`; it does not treat a shared secret by itself as authorization and never allows anonymous access to application endpoints. HU02 forwards the administrator token to the user-service role check, and HU06 forwards the proprietor token to the restaurant ownership check.

Local default storage is an in-memory H2 database on port `8082`. For MySQL, configure standard Spring datasource variables; for example:

```powershell
$env:JWT_SECRET = 'replace-with-the-same-random-secret-of-at-least-32-characters'
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/plazoleta_restaurantes'
$env:SPRING_DATASOURCE_USERNAME = '...'
$env:SPRING_DATASOURCE_PASSWORD = '...'
$env:SPRING_DATASOURCE_DRIVER = 'com.mysql.cj.jdbc.Driver'
$env:SPRING_JPA_DDL_AUTO = 'validate'
.\mvnw.cmd spring-boot:run
```

Other settings: `SERVER_PORT` (default `8082`), `USERS_ROLE_URL_TEMPLATE`, `H2_CONSOLE_ENABLED` (default `false`), and standard datasource variables. Use secrets from a secret manager outside local development. Production schema migration tooling and deployment configuration are not included.

## Tests

```powershell
.\mvnw.cmd clean verify
```

JUnit 5 / Mockito unit tests cover restaurant validation and forwarded owner-role checks, dish ownership and field-preserving updates/status changes, and pagination. Spring Boot/H2 + MockMvc tests exercise authentication and role protection, request validation, database-backed restaurant sorting/public fields, restaurant creation, default-active dish creation, and the protected HU06 ownership lookup.

## Architecture

- `domain`: restaurant/dish records, business exceptions and persistence/user-service ports; does not depend on infrastructure.
- `application`: use cases and ownership/business rules in `RestaurantService`.
- `infrastructure/input`: validated REST DTOs, controllers, MapStruct request mapping, security and error translation.
- `infrastructure/output`: JPA entities/adapters and the configurable HTTP user-role client.

MySQL is the runtime production driver; H2 provides local/test persistence. HU10 (dish listing) and all later challenge stories, user registration/authentication issuance, migrations, deployment and other microservices are not implemented here.
