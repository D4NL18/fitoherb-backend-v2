# Banners — Objetivos do Módulo

> **Data de criação:** 2026-09-23

---

## Objetivo Principal

Gerenciar os banners visuais da homepage do site Fitoherb, permitindo que administradores controlem o conteúdo visual publicitário sem necessidade de deploy.

## Objetivos Específicos

| # | Objetivo | Implementação |
|---|----------|--------------|
| 1 | Exibir banners ativos ordenados no site | `GET /banners/active` público, ordenado por `position` |
| 2 | Controlar visibilidade sem deletar | Campo `is_active` (ativar/desativar) |
| 3 | Definir ordem de exibição | Campo `position` (inteiro) |
| 4 | Gerenciar imagens de banner | Upload multipart via `FileStorageService` |
| 5 | Identificar banners de forma estável | UUID imutável (não slug) |
| 6 | Rastrear criação e modificação | Campos de auditoria automáticos |
