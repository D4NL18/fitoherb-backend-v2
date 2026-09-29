# Suppliers — Objetivos do Módulo

> **Data de criação:** 2026-09-23

---

## Objetivo Principal

Gerenciar os fornecedores/fabricantes parceiros da Fitoherb, permitindo que seus produtos sejam organizados por marca e que fornecedores destacados tenham visibilidade especial no site.

## Objetivos Específicos

| # | Objetivo | Implementação |
|---|----------|--------------|
| 1 | Listar fornecedores para filtros públicos | `GET /suppliers/get-all` público |
| 2 | Identificar fornecedores por URL amigável | Slug automático único |
| 3 | Permitir destaque de fornecedores parceiros | Campo `isHighlighted` |
| 4 | Gerenciar logotipo do fornecedor | Upload multipart via `FileStorageService` |
| 5 | Proteger integridade do catálogo | Restrição de FK em deleção |
| 6 | Deleção em cascata controlada | Parâmetro `deleteProducts=true` opcional |
