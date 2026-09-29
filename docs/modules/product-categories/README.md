# Product Categories — Visão Geral do Módulo

> **Data de criação:** 2026-09-23  
> **Controller:** `ProductCategoryController`  
> **Service:** `ProductCategoryService`

---

## Descrição

O módulo **Product Categories** gerencia as categorias que organizam o catálogo de produtos da Fitoherb. Cada categoria possui um nome único, slug gerado automaticamente e uma imagem de capa.

## Funcionalidades

| Funcionalidade | Endpoint | Acesso |
|---------------|----------|--------|
| Listar todas (sem paginação) | `GET /product_categories/get-all` | 🔓 Público |
| Listar paginada (admin) | `GET /product_categories` | 🔑 Autenticado |
| Buscar por slug | `GET /product_categories/{slug}` | 🔑 Autenticado |
| Criar categoria | `POST /product_categories` | 🔑 Autenticado |
| Atualizar categoria | `PUT /product_categories/{slug}` | 🔑 Autenticado |
| Deletar categoria | `DELETE /product_categories/{slug}` | 🔑 Autenticado |

## DTOs

| DTO | Campos |
|-----|--------|
| `ProductCategoryReq` | `name` |
| `ProductCategoryRes` | `name`, `slug`, `imageUrl` |
