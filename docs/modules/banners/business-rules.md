# Banners — Regras de Negócio

> **Data de criação:** 2026-09-23

---

## RN-BAN-001 — Endpoint de banners ativos é público

`GET /banners/active` não requer autenticação. Retorna apenas banners com `is_active = true`, ordenados por `position` (crescente).

## RN-BAN-002 — Listagem administrativa exige autenticação

`GET /banners`, `GET /banners/{id}`, `POST`, `PUT`, `DELETE` requerem token JWT válido (qualquer role autenticada).

## RN-BAN-003 — Imagem obrigatória na criação

`POST /banners` requer o part `image`. Sem imagem, retorna `400 Bad Request`.

## RN-BAN-004 — Imagem opcional na atualização

`PUT /banners/{id}` aceita `image` como opcional. Se omitida, mantém a imagem existente.

## RN-BAN-005 — Identificação por UUID

Banners são identificados por UUID (campo `id`). Não possuem slug. Todas as operações que referenciam um banner específico usam o ID.

## RN-BAN-006 — Campo position controla a ordem

O campo `position` (inteiro) determina a ordem de exibição dos banners ativos. Valor padrão: `0`. Banners com menor position aparecem primeiro.

## RN-BAN-007 — isActive controla visibilidade

O campo `is_active` (boolean, padrão `true`) permite desativar banners sem removê-los do banco. Apenas banners com `is_active = true` aparecem no endpoint público.

## RN-BAN-008 — Título obrigatório

O campo `title` é obrigatório e não pode ser vazio ou nulo. Validado via `@NotBlank`.

## RN-BAN-009 — Imagem deletada junto com o banner

`DELETE /banners/{id}` remove o registro do banco e a imagem do armazenamento via `FileStorageService.deleteBannerImage()`.

## RN-BAN-010 — Ordenação padrão da listagem admin é por position

`GET /banners` usa `sortField = position` como padrão. A API aceita também `title` e `createdAt`. O alias `name` é convertido internamente para `title`.
