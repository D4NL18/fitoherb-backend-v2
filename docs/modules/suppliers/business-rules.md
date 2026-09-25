# Suppliers — Regras de Negócio

> **Data de criação:** 2026-09-23

---

## RN-SUP-001 — Listagem pública sem paginação

`GET /suppliers/get-all` é público e retorna todos os fornecedores sem paginação. Usado para popular filtros e dropdowns no site e no painel.

## RN-SUP-002 — Nome de fornecedor único

Não podem existir dois fornecedores com o mesmo nome. Se já existir, o sistema retorna `409 Conflict`.

## RN-SUP-003 — Slug gerado automaticamente

O slug é gerado via `StringUtils.toSlug(name)` em `@PrePersist` e `@PreUpdate`. Não é informado pelo cliente.

## RN-SUP-004 — Slug único

O banco garante unicidade do slug via UNIQUE constraint. O serviço valida antes de salvar para retornar `409` informativo antes do erro de banco.

## RN-SUP-005 — Imagem obrigatória na criação

`POST /suppliers` requer o part `image` não nulo. Sem imagem, retorna `400`.

## RN-SUP-006 — Imagem opcional na atualização

`PUT /suppliers/{slug}` aceita `image` como opcional. Se omitida, mantém a imagem atual.

## RN-SUP-007 — Deleção com proteção de integridade

Por padrão, `DELETE /suppliers/{slug}` falha se o fornecedor possuir produtos vinculados, retornando `500` com mensagem explicativa.

## RN-SUP-008 — Deleção em cascata com flag

O parâmetro `deleteProducts=true` na query permite deletar o fornecedor e todos os seus produtos em cascata. Operação irreversível.

## RN-SUP-009 — Imagem deletada junto com o fornecedor

`DELETE /suppliers/{slug}` remove o logotipo do armazenamento via `FileStorageService.deleteSupplierImage()`.

## RN-SUP-010 — isHighlighted pode ser nulo

O campo `is_highlighted` é do tipo `Boolean` (nullable). O valor `null` significa "não definido" e diferencia-se de `false` ("explicitamente não destacado").
