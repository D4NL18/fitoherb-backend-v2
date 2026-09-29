# Auth — Contratos de API

> **Data de criação:** 2026-09-23  
> **Base path:** `/auth`

---

## POST /auth/login

**Descrição:** Autentica credenciais e retorna JWT. Define cookies de sessão.  
**Autenticação:** 🔓 Público  
**Content-Type:** `application/json`

### Request Body

```json
{
  "email": "admin@fitoherb.com.br",
  "password": "MinhaSenh@123",
  "rememberMe": true
}
```

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| `email` | string | ✅ | Formato e-mail válido, não vazio |
| `password` | string | ✅ | Não vazio |
| `rememberMe` | boolean | ❌ | — |

### Response 200 OK

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhdXRoLWFwaSIsInN1YiI6ImFkbWluQGZpdG9oZXJiLmNvbS5iciIsImV4cCI6MTc1ODcyMzYwMH0.abc123"
}
```

**Cookies definidos:**

| Cookie | HttpOnly | Duração |
|--------|----------|---------|
| `fitoherb_jwt` | ✅ Sim | 30 dias (rememberMe=true) ou sessão |
| `fitoherb_user_email` | ❌ Não | mesma duração |

### Respostas de Erro

| Status | Corpo |
|--------|-------|
| `400` | `{"status":"BAD_REQUEST","message":"Validation failed...","errors":{"email":"must be a well-formed email address"}}` |
| `401` | `{"status":"UNAUTHORIZED","message":"E-mail or password invalid."}` |

---

## POST /auth/register

**Descrição:** Registra novo usuário. Senha gerada pelo servidor e enviada por e-mail.  
**Autenticação:** 🛡️ Admin (`ROLE_ADMIN`)  
**Content-Type:** `application/json`

### Request Body

```json
{
  "email": "novo.usuario@fitoherb.com.br",
  "name": "Daniel Marinho",
  "role": "USER"
}
```

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| `email` | string | ✅ | E-mail válido, único no sistema |
| `name` | string | ✅ | Não vazio, 1–255 caracteres |
| `role` | enum | ✅ | `ADMIN` ou `USER` |

### Response 201 Created

```
HTTP/1.1 201 Created
Location: /auth/register/{uuid-do-usuario}
```
(sem body)

### Respostas de Erro

| Status | Corpo |
|--------|-------|
| `400` | `{"status":"BAD_REQUEST","message":"Validation failed...","errors":{"role":"must not be null"}}` |
| `409` | `{"status":"CONFLICT","message":"E-mail já está em uso"}` |
| `500` | `{"status":"INTERNAL_SERVER_ERROR","message":"Falha ao registrar usuário. O sistema não conseguiu salvar a conta."}` |

---

## POST /auth/refresh

**Descrição:** Renova o JWT sem re-autenticação. Aceita token expirado se assinatura válida.  
**Autenticação:** 🔓 Público (token via cookie ou header)  

### Fontes do token (em ordem de prioridade)

1. Cookie `fitoherb_jwt`
2. Header `Authorization: Bearer <token>`

### Response 200 OK

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Novos cookies de 30 dias também são definidos.

### Respostas de Erro

| Status | Situação |
|--------|---------|
| `400` | Nenhum token encontrado no cookie nem no header |

---

## POST /auth/logout

**Descrição:** Limpa os cookies de autenticação.  
**Autenticação:** 🔓 Público  

### Response 200 OK

(sem body)

**Cookies redefinidos com `MaxAge = 0`:**
- `fitoherb_jwt`
- `fitoherb_user_email`
