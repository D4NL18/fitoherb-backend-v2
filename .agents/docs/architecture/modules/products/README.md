# Products — Visão Geral do Módulo

> **Data de criação:** 2026-09-23  
> **Controller:** `ProductController`  
> **Service:** `ProductService`

---

## Descrição

O módulo **Products** é o núcleo do catálogo da Fitoherb. Gerencia o ciclo de vida completo dos produtos, incluindo upload de imagens, geração automática de slugs SEO-friendly, filtros avançados e uma galeria pública otimizada para o site.

## Funcionalidades

| Funcionalidade | Endpoint | Acesso |
|---------------|----------|--------|
| Galeria pública | `GET /products/gallery` | 🔓 Público |
| Listar (admin) | `GET /products` | 🔑 Autenticado |
| Buscar por slug | `GET /products/{slug}` | 🔑 Autenticado |
| Criar produto | `POST /products` | 🔑 Autenticado |
| Atualizar produto | `PUT /products/{slug}` | 🔑 Autenticado |
| Deletar produto | `DELETE /products/{slug}` | 🔑 Autenticado |

## Dependências

```
ProductController
    └── ProductService
            ├── ProductRepository          (consultas JPA)
            ├── ProductCategoryRepository  (lookup por slug de categoria)
            ├── SupplierRepository         (lookup por slug de fornecedor)
            ├── ProductMapper              (Entity → ProductRes)
            └── FileStorageService         (upload/delete de imagem)
```

## DTOs

| DTO | Uso | Campos |
|-----|-----|--------|
| `ProductReq` | Criação/atualização | `name`, `description`, `categorySlug`, `supplierSlug`, `flavours[]`, `presentation[]` |
| `ProductRes` | Resposta | `name`, `imageUrl`, `description`, `flavours[]`, `presentation[]`, `slug`, `category`, `supplier`, `createdAt` |

## Campos Especiais

- **`flavours`** — `List<String>` — sabores disponíveis (ex: Chocolate, Morango)
- **`presentation`** — `List<String>` — apresentações disponíveis (ex: 100g, 250g, 500g)
- **`slug`** — gerado automaticamente a partir do nome via `StringUtils.toSlug()`
- **`imageUrl`** — URL pública da imagem, construída no mapper a partir de `imagePath`
