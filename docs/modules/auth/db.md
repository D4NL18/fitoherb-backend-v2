# Auth — Banco de Dados

> **Data de criação:** 2026-09-23

---

O módulo Auth não possui tabela própria — utiliza a tabela `users` compartilhada com o módulo Users.

## Tabela `users` (visão do módulo Auth)

```sql
CREATE TABLE users (
    id          VARCHAR(36)  PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,   -- Identificador de login
    name        VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,           -- Hash BCrypt ($2a$10$...)
    role        VARCHAR(50)  NOT NULL,           -- 'ADMIN' | 'USER'
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);
```

## Campos relevantes para autenticação

| Campo | Tipo | Uso no Auth |
|-------|------|-------------|
| `email` | VARCHAR UNIQUE | Subject do JWT / chave de lookup no UserDetailsService |
| `password` | VARCHAR | Hash BCrypt comparado pelo `AuthenticationManager` |
| `role` | VARCHAR | Determina as `GrantedAuthority` do Spring Security |

## Enum UserRole → Authorities Spring Security

| role (DB) | Authorities concedidas |
|-----------|----------------------|
| `ADMIN` | `ROLE_ADMIN`, `ROLE_USER` |
| `USER` | `ROLE_USER` |

## Consultas executadas pelo módulo Auth

```sql
-- Carregar usuário por e-mail (UserDetailsService)
SELECT * FROM users WHERE email = ?;

-- Verificar e-mail duplicado no registro
SELECT COUNT(*) FROM users WHERE email = ?;

-- Inserir novo usuário
INSERT INTO users (id, email, name, password, role, created_at, created_by)
VALUES (?, ?, ?, ?, ?, NOW(), ?);
```

> 📝 O módulo Auth **nunca** armazena o token JWT — a validação é sempre stateless, feita pela assinatura criptográfica.
