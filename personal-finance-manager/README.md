Live link: https://personal-finance-manager-8whe.onrender.com/swagger-ui/index.html
# Personal Finance & Budget Manager API

A REST API built with **Java 17** and **Spring Boot 3** that helps users track income and
expenses, organize spending into categories, set monthly category budgets, and generate
financial reports — so they can finally see where their money is going.

---

## 1. Problem Statement

Most people don't have a clear, structured view of their monthly cash flow. They don't know:

- How much they've spent this month, and in which categories
- Whether they're on track against a budget they set for themselves
- Whether they're actually saving money month over month

Spreadsheets get abandoned. Banking apps show transactions but rarely enforce **budgets per
category**. This project solves that gap with a backend API that a mobile app, web app, or
personal script can build on top of.

## 2. Solution

The API lets each user:

1. Register/log in securely (JWT-based auth, BCrypt-hashed passwords).
2. Record income and expense **transactions**, each tied to a **category**.
3. Create custom categories in addition to the seeded defaults (Food, Travel, Shopping,
   Bills, Entertainment, Salary, Other).
4. Set a **monthly budget per category** and see, in real time, how much has been spent,
   how much remains, and whether they're `WITHIN_LIMIT`, `NEAR_LIMIT` (≥80% used), or
   `OVER_BUDGET`.
5. Pull **reports**: monthly income/expenses/savings, category-wise expense breakdown, and
   the highest spending category for the month.

Every piece of data is scoped to the authenticated user — no user can ever read, edit, or
delete another user's transactions, categories, or budgets.

## 3. Features

### Authentication
- Registration with email + password (BCrypt hashing)
- Login issuing a JWT access token
- Stateless JWT authentication on all protected endpoints

### Transactions
- Add income / add expense
- Update / delete / view a single transaction
- View transaction history with pagination and sorting
- Filter by category, transaction type, and date range

### Categories
- 7 default categories seeded automatically on registration
- Users can create their own custom categories
- Duplicate category names (case-insensitive) per user are rejected

### Budgets
- One budget per category per month/year
- Live-calculated: amount spent, remaining amount, percentage used, and status
  (`WITHIN_LIMIT` / `NEAR_LIMIT` / `OVER_BUDGET`)

### Reports
- `GET /api/reports/monthly` — income, expenses, savings, highest spending category
- `GET /api/reports/category-wise` — expense breakdown and % share per category

---

## 4. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Web | Spring Web (REST) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Security | Spring Security + JWT (JJWT) + BCrypt |
| Validation | Jakarta Bean Validation |
| Docs | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Boilerplate reduction | Lombok |
| Testing | JUnit 5, Mockito, Spring MockMvc, H2 (in-memory, tests only) |
| Containerization | Docker, Docker Compose |

No microservices, no Kafka, no Redis, no Kubernetes — this is intentionally a single,
well-structured monolith sized for a portfolio/interview project.

---

## 5. Architecture

```
Controller  →  Service  →  Repository  →  PostgreSQL
```

- **Controllers** only handle HTTP concerns (request/response, status codes) and delegate
  to services.
- **Services** hold all business logic (ownership checks, budget-status calculation,
  duplicate detection, report aggregation).
- **Repositories** are Spring Data JPA interfaces; filtering/aggregation queries use JPQL.
- **DTOs** are used everywhere at the API boundary — entities are never serialized directly.
- **JWT filter** runs once per request, resolves the user from the token, and populates the
  Spring Security context; every service method that reads/writes data takes the
  authenticated user's id and filters/validates ownership at the repository level.

### Package layout

```
com.example.finance
├── config       → SecurityConfig, OpenApiConfig, DataSeeder
├── controller    → REST controllers
├── dto
│   ├── request   → validated request bodies
│   └── response  → response payloads
├── entity        → JPA entities
├── exception     → custom exceptions + GlobalExceptionHandler
├── repository    → Spring Data JPA repositories
├── security      → JWT provider/filter, UserDetails, entry point
├── service       → business logic
├── util          → SecurityUtil, DefaultCategories
└── FinanceApplication.java
```

---

## 6. Database Design

| Table | Key columns | Notes |
|---|---|---|
| `users` | id, full_name, email (unique), password (hashed), created_at | |
| `roles` | id, name | `ROLE_USER`, `ROLE_ADMIN`, seeded on startup |
| `user_roles` | user_id, role_id | many-to-many join table |
| `categories` | id, name, default_category, user_id | unique (name, user_id) |
| `transactions` | id, amount, type, category_id, description, transaction_date, created_at, user_id | indexed on user_id and transaction_date |
| `budgets` | id, category_id, amount, month, year, user_id | unique (user_id, category_id, month, year) |

All financial amounts are stored as `NUMERIC(12,2)` (mapped via `BigDecimal`) to avoid
floating-point rounding issues.

---

## 7. API Documentation

Once running, interactive docs are at:

```
http://localhost:8080/swagger-ui.html
```

### Auth
| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |

### Transactions
| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/transactions` | Required |
| GET | `/api/transactions` | Required — supports `categoryId`, `type`, `startDate`, `endDate`, `page`, `size`, `sort` |
| GET | `/api/transactions/{id}` | Required |
| PUT | `/api/transactions/{id}` | Required |
| DELETE | `/api/transactions/{id}` | Required |

### Categories
| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/categories` | Required |
| GET | `/api/categories` | Required |

### Budgets
| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/budgets` | Required |
| GET | `/api/budgets` | Required — optional `month`, `year` filters |
| GET | `/api/budgets/{id}` | Required |

### Reports
| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/reports/monthly?month=8&year=2026` | Required |
| GET | `/api/reports/category-wise?month=8&year=2026` | Required |

All protected endpoints require the header:

```
Authorization: Bearer <accessToken>
```

### Sample requests/responses

**Register**
```
POST /api/auth/register
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "password": "securePass123"
}
```
```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "userId": 1,
  "fullName": "Jane Doe",
  "email": "jane@example.com"
}
```

**Create a transaction**
```
POST /api/transactions
Authorization: Bearer eyJhbGciOi...
{
  "amount": 500,
  "type": "EXPENSE",
  "categoryId": 2,
  "description": "Dinner",
  "transactionDate": "2026-08-10"
}
```

**Create a budget**
```
POST /api/budgets
{
  "categoryId": 2,
  "amount": 8000,
  "month": 8,
  "year": 2026
}
```
```json
{
  "id": 5,
  "categoryId": 2,
  "categoryName": "Food",
  "budgetAmount": 8000.00,
  "amountSpent": 500.00,
  "remainingAmount": 7500.00,
  "percentageUsed": 6.25,
  "status": "WITHIN_LIMIT",
  "month": 8,
  "year": 2026
}
```

**Monthly report**
```
GET /api/reports/monthly?month=8&year=2026
```
```json
{
  "month": 8,
  "year": 2026,
  "income": 60000.00,
  "expenses": 35000.00,
  "savings": 25000.00,
  "highestSpendingCategory": "Food",
  "highestSpendingAmount": 20000.00
}
```

---

## 8. Project Structure

```
personal-finance-manager/
├── src/
│   ├── main/
│   │   ├── java/com/example/finance/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/{request,response}/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   ├── service/
│   │   │   ├── util/
│   │   │   └── FinanceApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/com/example/finance/{controller,service}/
│       └── resources/application.properties
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── .gitignore
└── README.md
```

---

## 9. Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 14+ (or Docker, to run it in a container)

---

## 10. PostgreSQL Setup (local, without Docker)

```sql
CREATE DATABASE finance_db;
CREATE USER finance_user WITH ENCRYPTED PASSWORD 'finance_pass';
GRANT ALL PRIVILEGES ON DATABASE finance_db TO finance_user;
```

## 11. Environment Configuration

The app reads these environment variables (all have sensible local defaults in
`application.properties`):

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | finance_db | Database name |
| `DB_USERNAME` | finance_user | Database user |
| `DB_PASSWORD` | finance_pass | Database password |
| `JWT_SECRET` | (dev default — **override in production**) | HMAC signing key |
| `JWT_EXPIRATION_MS` | 86400000 (24h) | Token lifetime |
| `SERVER_PORT` | 8080 | HTTP port |

You can export these in your shell, or create a `.env` file and use `docker-compose` (which
already wires them for the containerized setup).

---

## 12. Installation & Running Locally

```bash
git clone <your-repo-url>
cd personal-finance-manager

# 1. Start PostgreSQL (see section 10) and make sure the env vars above match it

# 2. Build
mvn clean install

# 3. Run
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`, and Swagger UI at
`http://localhost:8080/swagger-ui.html`.

## 13. Running with Docker

```bash
docker-compose up --build
```

This starts PostgreSQL and the Spring Boot app together. The app waits for the database's
health check before starting. Once up:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

To stop:

```bash
docker-compose down
```

To also remove the database volume:

```bash
docker-compose down -v
```

## 14. Running Tests

```bash
mvn test
```

Tests run against an in-memory H2 database (PostgreSQL-compatible mode) — no external
database is needed to run the test suite. Coverage includes registration, login, transaction
CRUD, budget creation and status calculation, monthly report aggregation, and unauthorized
cross-user access attempts.

---

## 15. Example Workflow

1. `POST /api/auth/register` → get a JWT.
2. `GET /api/categories` → see the 7 default categories (Food, Travel, Shopping, Bills,
   Entertainment, Salary, Other) already created for you.
3. `POST /api/transactions` a few times — some `INCOME` (e.g. Salary), some `EXPENSE`
   (e.g. Food, Travel).
4. `POST /api/budgets` for "Food" with an amount for the current month.
5. `GET /api/budgets/{id}` → see live spend, remaining amount, % used, and status.
6. `GET /api/reports/monthly?month=8&year=2026` → see income, expenses, and savings.
7. `GET /api/reports/category-wise?month=8&year=2026` → see where the money went.

---

## 16. Future Improvements

- Refresh tokens + token revocation/blacklisting
- Recurring transactions (e.g. auto-log monthly rent)
- Multi-currency support
- CSV/PDF export of reports
- Email/push notifications when a budget crosses `NEAR_LIMIT` or `OVER_BUDGET`
- Admin role with cross-user reporting for household/shared budgets
- Rate limiting on auth endpoints

---

## Note on this build

This project was generated in a sandboxed environment without access to Maven Central, so
`mvn clean install` could not be executed here to produce a final verified build. The code
was written and manually reviewed for consistency (method signatures, DTO/entity field
alignment, JPQL correctness, Lombok-generated constructors, etc.), but **please run
`mvn clean install` and `mvn test` yourself** as the first step after cloning, and open an
issue/fix forward if anything surfaces — normal first-run friction with a project this size
(dependency versions, local PostgreSQL config) is expected.
