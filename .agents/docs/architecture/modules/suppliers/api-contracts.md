# Suppliers — Contratos de API

> **Data de criação:** 2026-09-23  
> **Base path:** `/suppliers`

---

## GET /suppliers/get-all

**Autenticação:** 🔓 Público  
**Descrição:** Todos os fornecedores sem paginação.

### Response 200 OK

```json
[
  {
    "name": "Fitoherb Natural",
    "slug": "fitoherb-natural",
    "imageUrl": "https://storage.googleapis.com/fitoherb-bucket/suppliers/fitoherb.jpg",
    "isHighlighted": true
  },
  {
    "name": "Vita Supplements",
    "slug": "vita-supplements",
    "imageUrl": "https://...",
    "isHighlighted": false
  }
]
```

---

## GET /suppliers

**Autenticação:** 🔑 Autenticado  

### Query Parameters

| Parâmetro | Padrão |
|-----------|--------|
| `search` | — |
| `page` | `0` |
| `sortField` | `name` |
| `direction` | `ASC` |

### Response 200 OK

```json
{
  "content": [{ "name": "Fitoherb Natural", "slug": "fitoherb-natural", "imageUrl": "...", "isHighlighted": true }],
  "totalElements": 8,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

---

## GET /suppliers/{slug}

**Autenticação:** 🔑 Autenticado  

### Response 200 OK

```json
{
  "name": "Fitoherb Natural",
  "slug": "fitoherb-natural",
  "imageUrl": "https://...",
  "isHighlighted": true
}
```

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | `{"status":"NOT_FOUND","message":"Supplier not found with slug: nature-labs"}` |

---

## POST /suppliers

**Autenticação:** 🔑 Autenticado  
**Content-Type:** `multipart/form-data`

### Parts

| Part | Content-Type | Obrigatório |
|------|-------------|-------------|
| `supplier` | `application/json` | ✅ |
| `image` | `image/*` | ✅ |

### Part `supplier` (JSON)

```json
{
  "name": "Natura Nordeste",
  "isHighlighted": true
}
```

| Campo | Obrigatório | Validação |
|-------|-------------|-----------|
| `name` | ✅ | Não vazio, único |
| `isHighlighted` | ❌ | boolean |

### Response 201 Created

```
Location: /suppliers/{uuid}
```

### Erros

| Status | Descrição |
|--------|-----------|
| `400` | Validação de campos |
| `409` | `{"status":"CONFLICT","message":"Supplier with that name already exists"}` |

---

## PUT /suppliers/{slug}

**Autenticação:** 🔑 Autenticado  
**Content-Type:** `multipart/form-data`

| Part | Obrigatório |
|------|-------------|
| `supplier` (JSON) | ✅ |
| `image` | ❌ |

### Response 200 OK

---

## DELETE /suppliers/{slug}

**Autenticação:** 🔑 Autenticado  

### Query Parameters

| Parâmetro | Tipo | Padrão | Descrição |
|-----------|------|--------|-----------|
| `deleteProducts` | boolean | `false` | Se `true`, deleta em cascata todos os produtos do fornecedor |

### Response 200 OK

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | Fornecedor não encontrado |
| `500` | `{"status":"INTERNAL_SERVER_ERROR","message":"Failed to delete supplier. Ensure there are no records linked to this account."}` |
