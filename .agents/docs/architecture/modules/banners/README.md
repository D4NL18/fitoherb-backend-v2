# Banners — Visão Geral do Módulo

> **Data de criação:** 2026-09-23  
> **Controller:** `BannerController`  
> **Service:** `BannerService`

---

## Descrição

O módulo **Banners** gerencia os banners exibidos na homepage do site Fitoherb. Cada banner possui título, imagem, flag de ativo/inativo e posição de exibição. O endpoint público `GET /banners/active` fornece os banners ativos ordenados para o frontend.

## Funcionalidades

| Funcionalidade | Endpoint | Acesso |
|---------------|----------|--------|
| Banners ativos (homepage) | `GET /banners/active` | 🔓 Público |
| Listar todos (admin) | `GET /banners` | 🔑 Autenticado |
| Buscar por ID | `GET /banners/{id}` | 🔑 Autenticado |
| Criar banner | `POST /banners` | 🔑 Autenticado |
| Atualizar banner | `PUT /banners/{id}` | 🔑 Autenticado |
| Deletar banner | `DELETE /banners/{id}` | 🔑 Autenticado |

## DTOs

| DTO | Campos |
|-----|--------|
| `BannerReq` | `title`, `isActive`, `position` |
| `BannerRes` | `id`, `title`, `imagePath`, `imageUrl`, `isActive`, `position`, `createdAt`, `updatedAt` |

## Diferença em relação aos outros módulos

Banners são identificados por **ID (UUID)** — não por slug. Isso porque banners são itens de configuração visual sem necessidade de URL SEO-friendly.
