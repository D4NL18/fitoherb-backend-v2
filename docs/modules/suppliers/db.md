# Suppliers — Banco de Dados

> **Data de criação:** 2026-09-23

---

## Tabela `suppliers`

```sql
CREATE TABLE suppliers (
    id             VARCHAR(36)  PRIMARY KEY,
    name           VARCHAR(255) NOT NULL UNIQUE,
    slug           VARCHAR(255) NOT NULL UNIQUE,    -- Gerado via @PrePersist
    image_path     VARCHAR(500),
    is_highlighted BOOLEAN,                         -- Nullable: null = não definido
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255)
);

CREATE INDEX idx_suppliers_slug         ON suppliers(slug);
CREATE INDEX idx_suppliers_name         ON suppliers(name);
CREATE INDEX idx_suppliers_highlighted  ON suppliers(is_highlighted);
```

## Descrição dos Campos

| Coluna | Tipo | Obrigatório | Descrição |
|--------|------|-------------|-----------|
| `id` | VARCHAR(36) | ✅ | UUID v4 |
| `name` | VARCHAR(255) | ✅ UNIQUE | Nome único do fornecedor |
| `slug` | VARCHAR(255) | ✅ UNIQUE | Slug gerado do nome |
| `image_path` | VARCHAR(500) | ❌ | Caminho/URL do logotipo |
| `is_highlighted` | BOOLEAN | ❌ (nullable) | Destaque na vitrine |
| `created_at` | TIMESTAMP | ✅ | Auditoria automática |
| `updated_at` | TIMESTAMP | ❌ | Auditoria automática |
| `created_by` | VARCHAR(255) | ❌ | Email do criador |
| `updated_by` | VARCHAR(255) | ❌ | Email do último modificador |

## Relações

```
suppliers.id ← products.supplier_id  (ONE_TO_MANY, via FK)
```

## Exemplo de Dados

```sql
INSERT INTO suppliers (id, name, slug, image_path, is_highlighted, created_at, created_by) VALUES
('s1000001-0000-0000-0000-000000000001', 'Fitoherb Natural', 'fitoherb-natural', 'suppliers/fitoherb.jpg', true, NOW(), 'admin@fitoherb.com.br'),
('s1000001-0000-0000-0000-000000000002', 'Vita Supplements', 'vita-supplements', 'suppliers/vita.jpg', false, NOW(), 'admin@fitoherb.com.br'),
('s1000001-0000-0000-0000-000000000003', 'Natura Nordeste', 'natura-nordeste', 'suppliers/natura.jpg', NULL, NOW(), 'admin@fitoherb.com.br');
```
