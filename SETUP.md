# Bank Account System - Setup Guide

## Prerequisites

- **Java 23** or higher
- **Maven 3.8.1** or higher
- **PostgreSQL 14** or higher
- **Git**
- **Postman** (for API testing)

## Database Setup

### 1. Create PostgreSQL Database and User

```sql
CREATE DATABASE bankdb;
CREATE USER bankuser WITH PASSWORD 'bankpass';
ALTER ROLE bankuser SET client_encoding TO 'utf8';
ALTER ROLE bankuser SET default_transaction_isolation TO 'read committed';
ALTER ROLE bankuser SET timezone TO 'UTC';
GRANT ALL PRIVILEGES ON DATABASE bankdb TO bankuser;
```

### 2. Verify Connection

```bash
psql -U bankuser -d bankdb -h localhost
```

## Application Setup

### 1. Clone Repository

```bash
git clone https://github.com/Janowski975/bank-account-system.git
cd bank-account-system
```

### 2. Configure Application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bankdb
spring.datasource.username=bankuser
spring.datasource.password=bankpass
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

server.port=8080

jwt.secret=your-secret-key-change-this-in-production
jwt.expiration=86400000
```

### 3. Build and Run

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

Application will be available at: **http://localhost:8080**

## API Testing with Postman

### 1. Import Postman Collection

- Open Postman
- Import collection (if available in repo)
- Set environment variables:
    - `baseUrl`: http://localhost:8080
    - `token`: (obtained after login)

### 2. API Endpoints Overview

#### Authentication
- **POST** `/api/auth/register` - Register new user
- **POST** `/api/auth/login` - Login and get JWT token

#### Accounts
- **GET** `/api/accounts` - List user accounts
- **POST** `/api/accounts` - Create new account
- **DELETE** `/api/accounts/{id}` - Delete account

#### Transactions
- **POST** `/api/accounts/{id}/deposit` - Deposit money
- **POST** `/api/transfers` - Transfer money between accounts

## Testing Workflow

### 1. Register User
```bash
POST http://localhost:8080/api/auth/register
Body:
{
  "username": "user1",
  "email": "user1@test.com",
  "password": "password123"
}
```

### 2. Login
```bash
POST http://localhost:8080/api/auth/login
Body:
{
  "username": "user1",
  "password": "password123"
}
Response: { "accessToken": "..." }
```

### 3. Create Account
```bash
POST http://localhost:8080/api/accounts
Headers: Authorization: Bearer [TOKEN]
Body:
{
  "accountNumber": "1234567890",
  "accountType": "SAVINGS",
  "currency": "PLN",
  "initialBalance": 0.00
}
```

### 4. Deposit
```bash
POST http://localhost:8080/api/accounts/{accountId}/deposit
Headers: Authorization: Bearer [TOKEN]
Body:
{
  "amount": 500.00,
  "description": "Initial deposit"
}
```

### 5. Transfer
```bash
POST http://localhost:8080/api/transfers
Headers: Authorization: Bearer [TOKEN]
Body:
{
  "fromAccountNumber": "1234567890",
  "toAccountNumber": "0987654321",
  "amount": 100.00,
  "description": "Transfer between accounts"
}
```

## Troubleshooting

### Database Connection Error
- Verify PostgreSQL is running
- Check database credentials in `application.properties`
- Ensure `bankdb` database exists

### Port 8080 Already in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

### Liquibase Migration Error
- Ensure database user has proper permissions
- Check `application.properties` configuration
- Review Liquibase changelog files

## IDE Configuration (IntelliJ IDEA)

1. Open project in IntelliJ
2. File → Project Structure → Project SDK → select Java 23
3. Maven: Enable auto-import
4. Run → Edit Configurations → Add Spring Boot Configuration
5. Main class: `com.bank.BankAccountSystemApplication`

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JWT Documentation](https://jwt.io/)

---

**Last Updated:** 2026-02-25