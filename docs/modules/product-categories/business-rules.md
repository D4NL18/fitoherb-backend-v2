# Product Categories — Regras de Negócio

> **Data de criação:** 2026-09-23

---

## RN-CAT-001 — Listagem pública sem paginação

`GET /product_categories/get-all` é público e retorna todas as categorias sem paginação. Destinado a popular filtros e dropdowns no site e painel.

## RN-CAT-002 — Nome de categoria único

Não podem existir duas categorias com o mesmo nome. Se já existir, retorna `409 Conflict`.

## RN-CAT-003 — Slug gerado automaticamente

O slug é gerado via `StringUtils.toSlug(name)` em `@PrePersist` e `@PreUpdate`. O cliente não informa o slug — ele é derivado do nome.

## RN-CAT-004 — Slug único no banco

Mesmo que dois nomes diferentes gerem o mesmo slug (ex: "Chás" e "Chas"), o banco garante a unicidade via UNIQUE constraint. O serviço valida antes de salvar para retornar `409` antes do erro de banco.

## RN-CAT-005 — Imagem obrigatória na criação

`POST /product_categories` requer o part `image`. Sem imagem, retorna `400 Bad Request`.

## RN-CAT-006 — Imagem opcional na atualização

`PUT /product_categories/{slug}` aceita `image` como opcional. Se omitida, a imagem atual é mantida.

## RN-CAT-007 — Deleção falha se houver produtos vinculados

Tentar deletar uma categoria com produtos associados causa erro de integridade referencial. O serviço pode interceptar e retornar `500` com mensagem informativa.

## RN-CAT-008 — Imagem deletada junto com a categoria

`DELETE /product_categories/{slug}` remove o registro do banco e a imagem do armazenamento via `FileStorageService.deleteCategoryImage()`.
