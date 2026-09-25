# Users — Banco de Dados

> **Data de criação:** 2026-09-23

---

## Tabela `users`

```sql
CREATE TABLE users (
    id          VARCHAR(36)  PRIMARY KEY,        -- UUID gerado pelo JPA
    email       VARCHAR(255) NOT NULL UNIQUE,    -- Identificador único de login
    name        VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,           -- Hash BCrypt ($2a$10$...)
    role        VARCHAR(50)  NOT NULL,           -- Enum: 'ADMIN' | 'USER'
    created_at  TIMESTAMP    NOT NULL,           -- @CreatedDate (imutável)
    updated_at  TIMESTAMP,                       -- @LastModifiedDate
    created_by  VARCHAR(255),                   -- @CreatedBy (email, imutável)
    updated_by  VARCHAR(255)                    -- @LastModifiedBy (email)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);
```

## Descrição dos Campos

| Coluna | Tipo PostgreSQL | Constraints | Descrição |
|--------|----------------|-------------|-----------|
| `id` | VARCHAR(36) | PK | UUID v4 gerado pelo JPA |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | E-mail de login e identificador único |
| `name` | VARCHAR(255) | NOT NULL | Nome completo do usuário |
| `password` | VARCHAR(255) | NOT NULL | Hash BCrypt (sempre começa com `$2a$`) |
| `role` | VARCHAR(50) | NOT NULL | `ADMIN` ou `USER` |
| `created_at` | TIMESTAMP | NOT NULL | Preenchido automaticamente na inserção |
| `updated_at` | TIMESTAMP | — | Atualizado automaticamente em cada UPDATE |
| `created_by` | VARCHAR(255) | — | Email do usuário que criou o registro |
| `updated_by` | VARCHAR(255) | — | Email do usuário que fez a última modificação |

## Exemplos de Dados

```sql
-- Usuário administrador
INSERT INTO users (id, email, name, password, role, created_at, created_by) VALUES
(
  'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
  'admin@fitoherb.com.br',
  'Administrador Fitoherb',
  '$2a$10$ExampleHashBCrypt...',
  'ADMIN',
  '2026-09-23 10:00:00',
  'system'
);

-- Usuário comum
INSERT INTO users (id, email, name, password, role, created_at, created_by) VALUES
(
  'b2c3d4e5-f6a7-8901-bcde-f23456789012',
  'maria@fitoherb.com.br',
  'Maria Silva',
  '$2a$10$AnotherHashBCrypt...',
  'USER',
  '2026-09-20 08:30:00',
  'admin@fitoherb.com.br'
);
```
