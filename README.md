# Bank Account Management System

[![Tests](https://github.com/Janowski975/bank-account-system/actions/workflows/tests.yml/badge.svg)](https://github.com/Janowski975/bank-account-system/actions)

REST API do zarządzania kontami bankowymi z autentykacją JWT.

## Opis

System zarządzania kontami bankowymi umożliwiający:
- Rejestrację i logowanie użytkowników (JWT)
- Tworzenie i zarządzanie kontami bankowymi
- Wykonywanie transakcji
- Przeglądanie historii transakcji

## Technologia

- **Backend:** Spring Boot 4.0.2, Spring Security 7.0.2, Spring Data JPA
- **Język:** Java 23
- **Baza danych:** PostgreSQL 18
- **ORM:** Hibernate (via Spring Data JPA)
- **Migracje:** Liquibase 5.0.1
- **Authentication:** JWT (JJWT 0.13.0)
- **Build:** Maven 3.9.12

## Wymagania

- Java 23 (testowane na Java 23, kompatybilne z Java 17+)
- PostgreSQL 18+
- Maven 3.8+ (masz 3.9.12 ✅)

## Szybki Start

### 1. Klonuj projekt

```bash
git clone <repo-url>
cd bank-account-system/bankapp
```

### 2. Zainstaluj PostgreSQL 18

**Windows:** https://www.postgresql.org/download/windows/

**Linux:**
```bash
sudo apt-get install postgresql-18
```

### 3. Utwórz użytkownika i bazę danych

```bash
psql -U postgres

CREATE USER bankuser WITH PASSWORD 'bankpass';
CREATE DATABASE bankdb OWNER bankuser;
GRANT ALL PRIVILEGES ON DATABASE bankdb TO bankuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO bankuser;
\q
```

### 4. Skonfiguruj aplikację

Edytuj `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bankdb
    username: bankuser
    password: bankpass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    enabled: true
```

### 5. Uruchom aplikację

```bash
mvn clean install
mvn spring-boot:run
```

Aplikacja będzie dostępna na: **http://localhost:8080/api**

---

## API Endpoints

### Autentykacja

#### Rejestracja
```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "janowski",
  "password": "test123",
  "email": "janowski@example.com"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiJ...",
  "type": "Bearer",
  "username": "janowski"
}
```

#### Logowanie
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "janowski",
  "password": "test123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiJ...",
  "type": "Bearer",
  "username": "janowski"
}
```

### Konta Bankowe

#### Pokaż wszystkie moje konta
```bash
GET /api/accounts
Authorization: Bearer <token>

Response: [
  {
    "id": 1,
    "accountNumber": "PL61109010140000071219812874",
    "accountName": "Moje główne konto",
    "balance": 5000.00,
    "accountType": "CHECKING"
  }
]
```

#### Utwórz nowe konto
```bash
POST /api/accounts
Authorization: Bearer <token>
Content-Type: application/json

{
  "accountName": "Konto oszczędnościowe",
  "accountType": "SAVINGS",
  "limit": 10000
}

Response: (jak wyżej)
```

#### Pokaż szczegóły konta
```bash
GET /api/accounts/{id}
Authorization: Bearer <token>
```

#### Edytuj konto
```bash
PUT /api/accounts/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "accountName": "Nowa nazwa",
  "accountType": "SAVINGS",
  "limit": 15000
}
```

#### Usuń konto
```bash
DELETE /api/accounts/{id}
Authorization: Bearer <token>
```

### Transakcje

#### Pokaż historię transakcji
```bash
GET /api/accounts/{accountId}/transactions?page=0&size=20
Authorization: Bearer <token>

Response: {
  "content": [
    {
      "id": 1,
      "amount": 100.00,
      "type": "TRANSFER",
      "referenceNumber": "REF123456789",
      "createdAt": "2026-02-04T12:30:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "currentPage": 0
}
```

#### Wykonaj transakcję
```bash
POST /api/accounts/{accountId}/transactions
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 100.00,
  "type": "TRANSFER",
  "description": "Przychodzący transfer"
}

Response: (jak wyżej)
```

---

## Architektura

```
src/main/java/pl/proggo/bankapp/
├── controller/           # REST Controllers
│   ├── AuthController.java
│   ├── AccountController.java
│   └── TransactionController.java
├── service/              # Business Logic
│   ├── AuthService.java
│   ├── AccountService.java
│   └── TransactionService.java
├── entity/               # Database Models (JPA)
│   ├── User.java
│   ├── Account.java
│   └── Transaction.java
├── dto/                  # Data Transfer Objects
│   ├── AuthResponse.java
│   ├── AccountDTO.java
│   └── TransactionDTO.java
├── repository/           # Data Access Layer
│   ├── UserRepository.java
│   ├── AccountRepository.java
│   └── TransactionRepository.java
├── security/             # JWT & Spring Security
│   ├── JwtUtils.java
│   ├── JwtAuthFilter.java
│   ├── SecurityConfig.java
│   └── CustomUserDetailsService.java
├── exception/            # Exception Handling
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── BusinessException.java
└── util/                 # Helper Classes
    ├── AccountNumberGenerator.java
    └── ReferenceNumberGenerator.java
```

## Funkcjonalności

✅ Rejestracja i logowanie z JWT  
✅ Zarządzanie kontami bankowymi (CRUD)  
✅ Historia transakcji ze stronnicowaniem  
✅ Walidacja danych wejściowych (Jakarta Validation)  
✅ Obsługa błędów (Global Exception Handler)  
✅ Logging (SLF4J)  
✅ Spring Security konfiguracja  
✅ PostgreSQL z Liquibase migracjami

## Bezpieczeństwo

- ✅ Spring Security konfiguracja
- ✅ JWT Token dla API requests (Bearer token)
- ✅ Hasła zahashowane (BCrypt)
- ✅ Dane użytkownika bezpieczne w bazie
- ✅ Role-based access control (RBAC)

## Testowanie

### Testowanie w Postmanie

1. Pobierz: https://www.postman.com/downloads/
2. Zaimportuj kolekcję (planowana)
3. Zaloguj się
4. Testuj endpointy

### Testowanie z curl

```bash
# Rejestracja
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123","email":"test@example.com"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# Pokaż konta (zamień TOKEN na token z logowania)
curl -X GET http://localhost:8080/api/accounts \
  -H "Authorization: Bearer TOKEN"
```

## Stack Techniczny (Szczegóły)

| Komponent | Wersja | Cel |
|-----------|--------|-----|
| Spring Boot | 4.0.2 | Web Framework |
| Spring Security | 7.0.2 | Autentykacja & Autoryzacja |
| Spring Data JPA | 4.0.2 | ORM (Object-Relational Mapping) |
| Hibernate | 7.2.0 | Database Entity Mapping |
| Jakarta Persistence | 3.2.0 | JPA API |
| PostgreSQL JDBC | 42.7.9 | Database Driver |
| JJWT | 0.13.0 | JWT Token Generation & Validation |
| Liquibase | 5.0.1 | Database Migracje (Version Control dla DB) |
| Lombok | 1.18.30 | Code Generation (@Getter, @Setter, itd.) |
| SLF4J | (included) | Logging Framework |
| Maven | 3.9.12 | Build Tool |
| Java | 23 | Programming Language |

## Continuous Integration

GitHub Actions automatically:
- ✅ Runs all tests on every push to master/main/develop
- ✅ Generates code coverage reports with JaCoCo
- ✅ Uploads coverage to Codecov
- ✅ Comments on Pull Requests with results

#### Run Tests Locally

```bash
mvn clean test
```

#### View Code Coverage
```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

See: [GitHub Actions](https://github.com/Janowski975/bank-account-system/actions)

## Licencja

MIT License - see LICENSE file

## Kontakt

Janowski975 - GitHub

---

**Status:** ✅ Production Ready (gotowy do pracy)

**Ostatnia aktualizacja:** Luty 2026