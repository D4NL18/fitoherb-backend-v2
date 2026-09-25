# Products — Objetivos do Módulo

> **Data de criação:** 2026-09-23

---

## Objetivo Principal

Gerenciar o catálogo completo de produtos da Fitoherb, oferecendo uma experiência de navegação rica para visitantes públicos e ferramentas de gestão eficientes para administradores.

## Objetivos Específicos

| # | Objetivo | Implementação |
|---|----------|--------------|
| 1 | Exibir catálogo público filtrado e paginado | `GET /products/gallery` público com filtros |
| 2 | Gerenciar produtos com imagem | `multipart/form-data` com `FileStorageService` |
| 3 | Garantir URLs SEO-friendly | Slug automático via `StringUtils.toSlug()` |
| 4 | Evitar produtos duplicados | Validação de nome e slug antes de persistir |
| 5 | Suportar variedades de produto | Arrays `flavours` e `presentation` em PostgreSQL |
| 6 | Vincular produtos a categorias e fornecedores | FK por slug com lookup antes de salvar |
| 7 | Manter integridade de imagens | Delete de imagem antiga ao atualizar/remover |
| 8 | Filtros compostos de múltiplos parâmetros | Query dinâmica por categoria[] e supplier[] |
