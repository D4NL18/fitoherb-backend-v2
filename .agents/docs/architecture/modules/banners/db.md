# Banners — Banco de Dados

> **Data de criação:** 2026-09-23

---

## Tabela `banners`

```sql
CREATE TABLE banners (
    id          VARCHAR(36)  PRIMARY KEY,        -- UUID gerado pelo JPA
    title       VARCHAR(255) NOT NULL,           -- Título obrigatório
    image_path  VARCHAR(500) NOT NULL,           -- Caminho/URL da imagem
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE, -- Visibilidade
    position    INTEGER      NOT NULL DEFAULT 0,    -- Ordem de exibição
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);

CREATE INDEX idx_banners_is_active ON banners(is_active);
CREATE INDEX idx_banners_position  ON banners(position);
```

## Descrição dos Campos

| Coluna | Tipo | Obrigatório | Padrão | Descrição |
|--------|------|-------------|--------|-----------|
| `id` | VARCHAR(36) | ✅ | — | UUID v4 gerado pelo JPA |
| `title` | VARCHAR(255) | ✅ | — | Título do banner |
| `image_path` | VARCHAR(500) | ✅ | — | Caminho/URL da imagem |
| `is_active` | BOOLEAN | ✅ | `true` | Visibilidade no site |
| `position` | INTEGER | ✅ | `0` | Ordem de exibição (crescente = primeiro) |
| `created_at` | TIMESTAMP | ✅ | AUTO | Auditoria automática |
| `updated_at` | TIMESTAMP | ❌ | AUTO | Auditoria automática |
| `created_by` | VARCHAR(255) | ❌ | — | Email do criador |
| `updated_by` | VARCHAR(255) | ❌ | — | Email do último modificador |

## Query dos Banners Ativos

```sql
-- Banners exibidos na homepage, ordenados por posição
SELECT * FROM banners
WHERE is_active = TRUE
ORDER BY position ASC;
```

## Exemplo de Dados

```sql
INSERT INTO banners (id, title, image_path, is_active, position, created_at, created_by) VALUES
('b0000001-0000-0000-0000-000000000001', 'Promoção de Inverno', 'banners/inverno.jpg', true, 0, NOW(), 'admin@fitoherb.com.br'),
('b0000001-0000-0000-0000-000000000002', 'Novos Produtos de Outono', 'banners/outono.jpg', true, 1, NOW(), 'admin@fitoherb.com.br'),
('b0000001-0000-0000-0000-000000000003', 'Banner Desativado', 'banners/antigo.jpg', false, 2, NOW(), 'admin@fitoherb.com.br');
```
