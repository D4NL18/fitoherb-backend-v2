# Banners — Contratos de API

> **Data de criação:** 2026-09-23  
> **Base path:** `/banners`

---

## GET /banners/active

**Autenticação:** 🔓 Público  
**Descrição:** Retorna todos os banners com `is_active = true`, ordenados por `position` ASC.

### Response 200 OK

```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "title": "Promoção de Inverno",
    "imagePath": "banners/inverno.jpg",
    "imageUrl": "https://storage.googleapis.com/fitoherb-bucket/banners/inverno.jpg",
    "isActive": true,
    "position": 0,
    "createdAt": "23-09-2026 10:00:00",
    "updatedAt": "23-09-2026 12:00:00"
  },
  {
    "id": "234e5678-f90a-23e4-b567-537725285111",
    "title": "Novos Produtos de Outono",
    "imagePath": "banners/outono.jpg",
    "imageUrl": "https://...",
    "isActive": true,
    "position": 1,
    "createdAt": "20-09-2026 08:00:00",
    "updatedAt": null
  }
]
```

---

## GET /banners

**Autenticação:** 🔑 Autenticado  
**Descrição:** Lista paginada de todos os banners para o painel.

### Query Parameters

| Parâmetro | Tipo | Padrão | Descrição |
|-----------|------|--------|-----------|
| `search` | string | — | Busca por título |
| `page` | int | `0` | Índice da página |
| `sortField` | string | `position` | `position`, `title`, `createdAt` |
| `direction` | string | `ASC` | `ASC` ou `DESC` |

### Response 200 OK

```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "title": "Promoção de Inverno",
      "imagePath": "banners/inverno.jpg",
      "imageUrl": "https://...",
      "isActive": true,
      "position": 0,
      "createdAt": "23-09-2026 10:00:00",
      "updatedAt": "23-09-2026 12:00:00"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

---

## GET /banners/{id}

**Autenticação:** 🔑 Autenticado  
**Descrição:** Detalhes de um banner pelo UUID.

### Path Parameters

| Parâmetro | Tipo | Validação |
|-----------|------|-----------|
| `id` | string | Não vazio |

### Response 200 OK

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "title": "Promoção de Inverno",
  "imagePath": "banners/inverno.jpg",
  "imageUrl": "https://...",
  "isActive": true,
  "position": 0,
  "createdAt": "23-09-2026 10:00:00",
  "updatedAt": "23-09-2026 12:00:00"
}
```

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | `{"status":"NOT_FOUND","message":"Banner not found with id: 123"}` |

---

## POST /banners

**Autenticação:** 🔑 Autenticado  
**Content-Type:** `multipart/form-data`

### Parts

| Part | Content-Type | Obrigatório |
|------|-------------|-------------|
| `banner` | `application/json` | ✅ |
| `image` | `image/*` | ✅ |

### Part `banner` (JSON)

```json
{
  "title": "Promoção de Inverno",
  "isActive": true,
  "position": 0
}
```

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| `title` | string | ✅ | Não vazio, 1–255 chars |
| `isActive` | boolean | ❌ | Padrão: `true` |
| `position` | int | ❌ | Padrão: `0` |

### Response 201 Created

```
HTTP/1.1 201 Created
Location: /banners/{uuid}
```

### Erros

| Status | Descrição |
|--------|-----------|
| `400` | `{"status":"BAD_REQUEST","message":"Validation failed...","errors":{"title":"This field cannot be empty or null"}}` |
| `500` | `{"status":"INTERNAL_SERVER_ERROR","message":"Failed to save banner."}` |

---

## PUT /banners/{id}

**Autenticação:** 🔑 Autenticado  
**Content-Type:** `multipart/form-data`

### Parts

| Part | Obrigatório |
|------|-------------|
| `banner` (JSON) | ✅ |
| `image` | ❌ |

### Response 200 OK

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | Banner não encontrado |
| `500` | Falha ao atualizar no banco |

---

## DELETE /banners/{id}

**Autenticação:** 🔑 Autenticado  

### Response 200 OK

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | `{"status":"NOT_FOUND","message":"Banner not found with id: 123"}` |
| `500` | `{"status":"INTERNAL_SERVER_ERROR","message":"Failed to delete banner."}` |
