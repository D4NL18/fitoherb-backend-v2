# Product Categories — Objetivos do Módulo

> **Data de criação:** 2026-09-23

---

## Objetivo Principal

Prover a estrutura de categorização do catálogo Fitoherb, permitindo que produtos sejam organizados tematicamente e que clientes possam filtrar a galeria por categoria.

## Objetivos Específicos

| # | Objetivo | Implementação |
|---|----------|--------------|
| 1 | Organizar produtos em categorias temáticas | FK `category_id` na tabela `products` |
| 2 | Prover lista completa para filtros públicos | `GET /product_categories/get-all` público sem paginação |
| 3 | Garantir URLs únicas para cada categoria | Slug automático com unicidade no banco |
| 4 | Suportar imagens de capa das categorias | Upload via multipart, storage via `FileStorageService` |
| 5 | Evitar categorias duplicadas | Validação de nome único antes de persistir |
| 6 | Manter integridade com o catálogo | Restrição de FK impede deleção de categorias em uso |
