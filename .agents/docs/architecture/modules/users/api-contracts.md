# Users — Contratos de API

> **Data de criação:** 2026-09-23  
> **Base path:** `/users`

---

## GET /users

**Autenticação:** 🔑 Autenticado  
**Descrição:** Lista paginada de usuários com busca e ordenação dinâmica.

### Query Parameters

| Parâmetro | Tipo | Padrão | Descrição |
|-----------|------|--------|-----------|
| `search` | string | — | Filtra por nome ou e-mail (case-insensitive) |
| `page` | int | `0` | Índice da página (base 0) |
| `sortField` | string | `name` | `name`, `email`, `role`, `createdAt` |
| `direction` | string | `ASC` | `ASC` ou `DESC` |

### Response 200 OK

```json
{
  "content": [
    {
      "email": "admin@fitoherb.com.br",
      "name": "Administrador Fitoherb",
      "role": "ADMIN",
      "createdAt": "23-09-2026 10:00:00"
    },
    {
      "email": "usuario@fitoherb.com.br",
      "name": "Maria Silva",
      "role": "USER",
      "createdAt": "20-09-2026 08:30:00"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "first": true,
  "last": true
}
```

---

## GET /users/{email}

**Autenticação:** 🔑 Autenticado  
**Descrição:** Retorna perfil do usuário pelo e-mail.

### Path Parameters

| Parâmetro | Tipo | Validação |
|-----------|------|-----------|
| `email` | string | Formato e-mail válido, não nulo |

### Response 200 OK

```json
{
  "email": "daniel@fitoherb.com.br",
  "name": "Daniel Marinho",
  "role": "ADMIN",
  "createdAt": "23-09-2026 10:00:00"
}
```

### Erros

| Status | Descrição |
|--------|-----------|
| `401` | Token ausente ou inválido |
| `404` | `{"status":"NOT_FOUND","message":"User not found with email: user@example.com"}` |

---

## PUT /users/{email}

**Autenticação:** 🛡️ Admin  
**Descrição:** Atualiza nome e/ou role do usuário (não altera senha).  
**Content-Type:** `application/json`

### Request Body

```json
{
  "name": "Daniel Marinho Silva",
  "role": "ADMIN"
}
```

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| `name` | string | ✅ | Não vazio, 1–255 caracteres |
| `role` | enum | ✅ | `ADMIN` ou `USER` |

### Response 200 OK

(sem body)

### Erros

| Status | Descrição |
|--------|-----------|
| `400` | `{"status":"BAD_REQUEST","message":"Validation failed...","errors":{"name":"must not be blank"}}` |
| `403` | `{"status":"FORBIDDEN","message":"Access denied..."}` |
| `404` | `{"status":"NOT_FOUND","message":"User not found with email: admin@example.com"}` |
| `500` | `{"status":"INTERNAL_SERVER_ERROR","message":"Failed to update user in database."}` |

---

## PATCH /users/update-password/{email}

**Autenticação:** 🔑 Autenticado  
**Descrição:** Atualiza a senha da conta especificada.  
**Content-Type:** `application/json`

### Request Body

```json
{
  "password": "NovaSenha@456"
}
```

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| `password` | string | ✅ | 8–100 caracteres |

### Response 200 OK

(sem body)

### Erros

| Status | Descrição |
|--------|-----------|
| `400` | `{"status":"BAD_REQUEST","message":"Validation failed...","errors":{"password":"size must be between 8 and 100"}}` |
| `401` | Token inválido |
| `404` | Usuário não encontrado |
| `500` | Falha ao salvar nova senha |

---

## DELETE /users/{email}

**Autenticação:** 🛡️ Admin  
**Descrição:** Remove permanentemente a conta do usuário.

### Response 200 OK

(sem body)

### Erros

| Status | Descrição |
|--------|-----------|
| `403` | Sem permissão de admin |
| `404` | Usuário não encontrado |
| `500` | `{"status":"INTERNAL_SERVER_ERROR","message":"Failed to delete user. Ensure there are no records linked to this account."}` |
