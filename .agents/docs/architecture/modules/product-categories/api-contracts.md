# Product Categories — Contratos de API

> **Data de criação:** 2026-09-23  
> **Base path:** `/product_categories`

---

## GET /product_categories/get-all

**Autenticação:** 🔓 Público  
**Descrição:** Retorna todas as categorias sem paginação.

### Response 200 OK

```json
[
  {
    "name": "Chás e Infusões",
    "slug": "chas-e-infusoes",
    "imageUrl": "https://storage.googleapis.com/fitoherb-bucket/categories/chas.jpg"
  },
  {
    "name": "Suplementos",
    "slug": "suplementos",
    "imageUrl": "https://storage.googleapis.com/fitoherb-bucket/categories/suplementos.jpg"
  }
]
```

---

## GET /product_categories

**Autenticação:** 🔑 Autenticado  
**Descrição:** Listagem paginada para o painel.

### Query Parameters

| Parâmetro | Tipo | Padrão |
|-----------|------|--------|
| `search` | string | — |
| `page` | int | `0` |
| `sortField` | string | `name` |
| `direction` | string | `ASC` |

### Response 200 OK

```json
{
  "content": [
    {
      "name": "Chás e Infusões",
      "slug": "chas-e-infusoes",
      "imageUrl": "https://..."
    }
  ],
  "totalElements": 12,
  "totalPages": 2,
  "size": 10,
  "number": 0
}
```

---

## GET /product_categories/{slug}

**Autenticação:** 🔑 Autenticado  

### Response 200 OK

```json
{
  "name": "Chás e Infusões",
  "slug": "chas-e-infusoes",
  "imageUrl": "https://..."
}
```

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | `{"status":"NOT_FOUND","message":"Category not found with slug: chas-e-infusoes"}` |

---

## POST /product_categories

**Autenticação:** 🔑 Autenticado  
**Content-Type:** `multipart/form-data`

### Parts

| Part | Content-Type | Obrigatório |
|------|-------------|-------------|
| `category` | `application/json` | ✅ |
| `image` | `image/*` | ✅ |

### Part `category` (JSON)

```json
{
  "name": "Probióticos"
}
```

| Campo | Obrigatório | Validação |
|-------|-------------|-----------|
| `name` | ✅ | Não vazio, 1–255 chars, único |

### Response 201 Created

```
HTTP/1.1 201 Created
Location: /product_categories/{uuid}
```

### Erros

| Status | Descrição |
|--------|-----------|
| `400` | Validação falhou |
| `409` | Categoria com mesmo nome ou slug já existe |

---

## PUT /product_categories/{slug}

**Autenticação:** 🔑 Autenticado  
**Content-Type:** `multipart/form-data`

| Part | Obrigatório |
|------|-------------|
| `category` (JSON) | ✅ |
| `image` | ❌ |

### Response 200 OK

---

## DELETE /product_categories/{slug}

**Autenticação:** 🔑 Autenticado  

### Response 200 OK

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | Categoria não encontrada |
| `500` | Integridade referencial (produtos vinculados) |
