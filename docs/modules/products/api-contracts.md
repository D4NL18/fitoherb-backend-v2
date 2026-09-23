# Products — Contratos de API

> **Data de criação:** 2026-09-23  
> **Base path:** `/products`

---

## GET /products/gallery

**Autenticação:** 🔓 Público  
**Descrição:** Galeria pública de produtos para o site da Fitoherb.

### Query Parameters

| Parâmetro | Tipo | Padrão | Descrição |
|-----------|------|--------|-----------|
| `search` | string | — | Busca por nome do produto |
| `category` | string[] | — | Slugs de categorias (ex: `chas-e-infusoes,suplementos`) |
| `supplier` | string[] | — | Slugs de fornecedores |
| `page` | int | `0` | Índice da página |
| `size` | int | `15` | Itens por página |
| `direction` | string | `ASC` | `ASC` ou `DESC` |

### Response 200 OK

```json
{
  "content": [
    {
      "name": "Chá de Camomila Orgânico",
      "imageUrl": "https://storage.googleapis.com/fitoherb-bucket/products/camomila.jpg",
      "description": "Flores de camomila desidratadas, ideais para infusões relaxantes.",
      "flavours": ["Natural", "Mel"],
      "presentation": ["100g", "250g", "500g"],
      "slug": "cha-de-camomila-organico",
      "category": {
        "name": "Chás e Infusões",
        "slug": "chas-e-infusoes",
        "imageUrl": "https://..."
      },
      "supplier": {
        "name": "Fitoherb Natural",
        "slug": "fitoherb-natural",
        "imageUrl": "https://...",
        "isHighlighted": true
      },
      "createdAt": "23-09-2026 10:00:00"
    }
  ],
  "totalElements": 120,
  "totalPages": 8,
  "size": 15,
  "number": 0,
  "first": true,
  "last": false
}
```

### Erros

| Status | Situação |
|--------|---------|
| `404` | Categoria ou fornecedor não encontrado pelo slug nos filtros |

---

## GET /products

**Autenticação:** 🔑 Autenticado  
**Descrição:** Listagem paginada para o painel administrativo.

### Query Parameters

| Parâmetro | Tipo | Padrão |
|-----------|------|--------|
| `search` | string | — |
| `category` | string[] | — |
| `supplier` | string[] | — |
| `page` | int | `0` |
| `sortField` | string | `name` |
| `direction` | string | `ASC` |

### Response 200 OK

Mesmo formato da galeria, 10 itens por página padrão.

---

## GET /products/{slug}

**Autenticação:** 🔑 Autenticado  
**Descrição:** Detalhes de um produto pelo slug.

### Path Parameters

| Parâmetro | Validação |
|-----------|-----------|
| `slug` | Regex slug válido (letras minúsculas, dígitos, hífens) |

### Response 200 OK

```json
{
  "name": "Chá de Camomila Orgânico",
  "imageUrl": "https://...",
  "description": "Flores de camomila desidratadas...",
  "flavours": ["Natural"],
  "presentation": ["100g", "250g"],
  "slug": "cha-de-camomila-organico",
  "category": { "name": "Chás e Infusões", "slug": "chas-e-infusoes", "imageUrl": "..." },
  "supplier": { "name": "Fitoherb Natural", "slug": "fitoherb-natural", "imageUrl": "...", "isHighlighted": true },
  "createdAt": "23-09-2026 10:00:00"
}
```

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | `{"status":"NOT_FOUND","message":"Product not found with slug: chamomile-tea"}` |
| `403` | Sem autenticação |

---

## POST /products

**Autenticação:** 🔑 Autenticado  
**Content-Type:** `multipart/form-data`

### Parts

| Part | Content-Type | Obrigatório |
|------|-------------|-------------|
| `product` | `application/json` | ✅ |
| `image` | `image/*` | ✅ |

### Part `product` (JSON)

```json
{
  "name": "Chá de Camomila Orgânico",
  "description": "Flores de camomila desidratadas, ideais para infusões relaxantes antes de dormir.",
  "categorySlug": "chas-e-infusoes",
  "supplierSlug": "fitoherb-natural",
  "flavours": ["Natural", "Mel"],
  "presentation": ["100g", "250g", "500g"]
}
```

| Campo | Tipo | Obrigatório | Validação |
|-------|------|-------------|-----------|
| `name` | string | ✅ | Não vazio, 1–255 caracteres |
| `description` | string | ❌ | Máximo 5000 caracteres |
| `categorySlug` | string | ✅ | Slug de categoria existente |
| `supplierSlug` | string | ✅ | Slug de fornecedor existente |
| `flavours` | string[] | ❌ | Máximo 50 itens |
| `presentation` | string[] | ❌ | Máximo 50 itens |

### Response 201 Created

```
HTTP/1.1 201 Created
Location: /products/{uuid}
```

### Erros

| Status | Descrição |
|--------|-----------|
| `400` | `{"status":"BAD_REQUEST","message":"Validation failed...","errors":{"name":"must not be blank","categorySlug":"must not be null"}}` |
| `404` | `{"status":"NOT_FOUND","message":"Category not found with slug: organic-teas"}` |
| `409` | `{"status":"CONFLICT","message":"Product with that name already exists"}` |

---

## PUT /products/{slug}

**Autenticação:** 🔑 Autenticado  
**Content-Type:** `multipart/form-data`

### Parts

| Part | Obrigatório | Descrição |
|------|-------------|-----------|
| `product` | ✅ | JSON com dados atualizados (mesmo schema do POST) |
| `image` | ❌ | Nova imagem (se omitida, mantém a existente) |

### Response 200 OK

(sem body)

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | Produto não encontrado |
| `409` | `{"status":"CONFLICT","message":"A product with a similar name already exists (Slug conflict: new-slug)"}` |
| `500` | Falha ao atualizar no banco |

---

## DELETE /products/{slug}

**Autenticação:** 🔑 Autenticado

### Response 200 OK

(sem body)

### Erros

| Status | Descrição |
|--------|-----------|
| `404` | `{"status":"NOT_FOUND","message":"Product not found with slug: chamomile-tea"}` |
| `500` | Erro de integridade ou falha de storage |
