# Suppliers — Visão Geral do Módulo

> **Data de criação:** 2026-09-23  
> **Controller:** `SupplierController`  
> **Service:** `SupplierService`

---

## Descrição

O módulo **Suppliers** gerencia os fornecedores (fabricantes/distribuidores) cujos produtos são comercializados pela Fitoherb. Cada fornecedor possui logotipo e pode ser marcado como "destacado" para exibição privilegiada no site.

## Funcionalidades

| Funcionalidade | Endpoint | Acesso |
|---------------|----------|--------|
| Listar todos (sem paginação) | `GET /suppliers/get-all` | 🔓 Público |
| Listar paginada (admin) | `GET /suppliers` | 🔑 Autenticado |
| Buscar por slug | `GET /suppliers/{slug}` | 🔑 Autenticado |
| Criar fornecedor | `POST /suppliers` | 🔑 Autenticado |
| Atualizar fornecedor | `PUT /suppliers/{slug}` | 🔑 Autenticado |
| Deletar fornecedor | `DELETE /suppliers/{slug}` | 🔑 Autenticado |

## DTOs

| DTO | Campos |
|-----|--------|
| `SupplierReq` | `name`, `isHighlighted` |
| `SupplierRes` | `name`, `slug`, `imageUrl`, `isHighlighted` |

## Campo Especial: `isHighlighted`

O campo booleano `is_highlighted` permite marcar fornecedores para destaque na página inicial ou em seções especiais do site. Valor padrão: `null` (não definido).
