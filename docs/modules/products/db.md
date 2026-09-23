# Products — Banco de Dados

> **Data de criação:** 2026-09-23

---

## Tabela `products`

```sql
CREATE TABLE products (
    id           VARCHAR(36)  PRIMARY KEY,        -- UUID gerado pelo JPA
    name         VARCHAR(255) NOT NULL,
    slug         VARCHAR(255) NOT NULL UNIQUE,    -- Gerado via @PrePersist
    image_path   VARCHAR(500),                   -- Caminho/URL da imagem
    description  TEXT,                           -- Descrição longa
    flavours     TEXT[],                         -- Array de sabores
    presentation TEXT[],                         -- Array de apresentações
    category_id  VARCHAR(36)  NOT NULL,
    supplier_id  VARCHAR(36)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),

    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES product_categories(id),
    CONSTRAINT fk_products_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE INDEX idx_products_slug        ON products(slug);
CREATE INDEX idx_products_name        ON products(name);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_supplier_id ON products(supplier_id);
```

## Descrição dos Campos

| Coluna | Tipo | Obrigatório | Descrição |
|--------|------|-------------|-----------|
| `id` | VARCHAR(36) | ✅ | UUID v4 |
| `name` | VARCHAR(255) | ✅ | Nome do produto |
| `slug` | VARCHAR(255) | ✅ UNIQUE | Slug gerado automaticamente do name |
| `image_path` | VARCHAR(500) | ❌ | Caminho relativo ou URL da imagem |
| `description` | TEXT | ❌ | Descrição detalhada (sem limite de tamanho) |
| `flavours` | TEXT[] | ❌ | Array PostgreSQL de sabores |
| `presentation` | TEXT[] | ❌ | Array PostgreSQL de apresentações |
| `category_id` | VARCHAR(36) | ✅ | FK → `product_categories.id` |
| `supplier_id` | VARCHAR(36) | ✅ | FK → `suppliers.id` |
| `created_at` | TIMESTAMP | ✅ | Auditoria — criado em |
| `updated_at` | TIMESTAMP | ❌ | Auditoria — modificado em |
| `created_by` | VARCHAR(255) | ❌ | Auditoria — criado por |
| `updated_by` | VARCHAR(255) | ❌ | Auditoria — modificado por |

## Exemplo de Dado

```sql
INSERT INTO products (
    id, name, slug, image_path, description,
    flavours, presentation, category_id, supplier_id,
    created_at, created_by
) VALUES (
    'c3d4e5f6-a7b8-9012-cdef-345678901234',
    'Chá de Camomila Orgânico',
    'cha-de-camomila-organico',
    'products/camomila.jpg',
    'Flores de camomila desidratadas, ideais para infusões relaxantes antes de dormir.',
    ARRAY['Natural', 'Mel'],
    ARRAY['100g', '250g', '500g'],
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',  -- category_id
    'b2c3d4e5-f6a7-8901-bcde-f23456789012',  -- supplier_id
    NOW(),
    'admin@fitoherb.com.br'
);
```

## Relações

```
products.category_id → product_categories.id  (MANY_TO_ONE, LAZY)
products.supplier_id → suppliers.id           (MANY_TO_ONE, LAZY)
```

> **⚠️ Integridade referencial:** Não é possível deletar uma `ProductCategory` ou `Supplier` enquanto existirem produtos vinculados. A deleção de fornecedor com a flag `deleteProducts=true` remove os produtos em cascata via lógica no serviço.
