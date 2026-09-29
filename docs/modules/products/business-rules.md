# Products — Regras de Negócio

> **Data de criação:** 2026-09-23

---

## RN-PROD-001 — Galeria pública sem autenticação

`GET /products/gallery` é um endpoint público. Não requer token JWT. Serve para exibir o catálogo no site da Fitoherb para visitantes anônimos.

## RN-PROD-002 — Listagem administrativa requer autenticação

`GET /products` e `GET /products/{slug}` requerem autenticação (qualquer role). São destinados ao painel de gestão.

## RN-PROD-003 — Criação exige imagem obrigatória

`POST /products` requer o part `image` não nulo no multipart. Sem imagem, retorna `400 Bad Request`.

## RN-PROD-004 — Atualização permite imagem opcional

`PUT /products/{slug}` aceita o part `image` como opcional. Se omitido, mantém a imagem existente.

## RN-PROD-005 — Slug gerado automaticamente do nome

O slug é calculado via `StringUtils.toSlug(name)` em `@PrePersist` e `@PreUpdate`. O cliente nunca informa o slug diretamente — ele é sempre derivado do nome.

## RN-PROD-006 — Conflito de nome e slug detectado antes de salvar

Antes de criar um produto, o `ProductService` verifica:
1. Se já existe produto com o mesmo **nome** exato → `409 Conflict`
2. Se o **slug** gerado já existe (mesmo nome com variação de acentuação) → `409 Conflict`

## RN-PROD-007 — Categoria e fornecedor referenciados por slug

O `ProductReq` recebe `categorySlug` e `supplierSlug`. O serviço realiza lookup por slug no banco. Se não encontrado, retorna `404 Not Found`.

## RN-PROD-008 — Imagem anterior deletada ao atualizar

Quando uma nova imagem é fornecida no `PUT`, a imagem anterior é removida do armazenamento (local ou GCS) antes de salvar a nova.

## RN-PROD-009 — Imagem deletada junto com o produto

`DELETE /products/{slug}` remove o registro do banco **e** a imagem do armazenamento.

## RN-PROD-010 — Paginação da galeria com tamanho configurável

`GET /products/gallery` aceita o parâmetro `size` (padrão: 15). A listagem administrativa usa tamanho fixo de 10.

## RN-PROD-011 — Filtros compostos por categoria e fornecedor

Ambos os endpoints de listagem suportam filtro simultâneo por múltiplos slugs de categoria e fornecedor via parâmetros de query (listas).

## RN-PROD-012 — Slug validado via regex na URL

O path variable `{slug}` é validado contra `SLUG_REGEX` no controller. Slugs inválidos retornam `400` sem chegar ao serviço.

## RN-PROD-013 — Description nula se vazia

O setter de `description` em `ProductReq` converte string vazia ou apenas espaços para `null`, evitando strings vazias no banco.
