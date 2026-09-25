# Product Categories — Banco de Dados

> **Data de criação:** 2026-09-23

---

## Tabela `product_categories`

```sql
CREATE TABLE product_categories (
    id          VARCHAR(36)  PRIMARY KEY,        -- UUID gerado pelo JPA
    name        VARCHAR(255) NOT NULL UNIQUE,    -- Nome único da categoria
    slug        VARCHAR(255) NOT NULL UNIQUE,    -- Gerado via @PrePersist
    image_path  VARCHAR(500),                   -- Caminho/URL da imagem de capa
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);

CREATE INDEX idx_product_categories_slug ON product_categories(slug);
CREATE INDEX idx_product_categories_name ON product_categories(name);
```

## Descrição dos Campos

| Coluna | Tipo | Obrigatório | Descrição |
|--------|------|-------------|-----------|
| `id` | VARCHAR(36) | ✅ | UUID v4 |
| `name` | VARCHAR(255) | ✅ UNIQUE | Nome único da categoria |
| `slug` | VARCHAR(255) | ✅ UNIQUE | Slug gerado do nome (ex: "chas-e-infusoes") |
| `image_path` | VARCHAR(500) | ❌ | Caminho relativo ou URL da imagem |
| `created_at` | TIMESTAMP | ✅ | Auditoria automática |
| `updated_at` | TIMESTAMP | ❌ | Auditoria automática |
| `created_by` | VARCHAR(255) | ❌ | Email do criador |
| `updated_by` | VARCHAR(255) | ❌ | Email do último modificador |

## Relações

```
product_categories.id ← products.category_id  (ONE_TO_MANY, via FK)
```

## Exemplo de Dados

```sql
INSERT INTO product_categories (id, name, slug, image_path, created_at, created_by) VALUES
('a1b2c3d4-0001-0001-0001-000000000001', 'Chás e Infusões', 'chas-e-infusoes', 'categories/chas.jpg', NOW(), 'admin@fitoherb.com.br'),
('a1b2c3d4-0001-0001-0001-000000000002', 'Suplementos', 'suplementos', 'categories/suplementos.jpg', NOW(), 'admin@fitoherb.com.br'),
('a1b2c3d4-0001-0001-0001-000000000003', 'Óleos Essenciais', 'oleos-essenciais', 'categories/oleos.jpg', NOW(), 'admin@fitoherb.com.br');
```
