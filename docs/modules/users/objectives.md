# Users — Objetivos do Módulo

> **Data de criação:** 2026-09-23

---

## Objetivo Principal

Prover gerenciamento completo do ciclo de vida de contas de usuário, garantindo que dados sensíveis (senha) nunca sejam expostos e que modificações destrutivas (update/delete) exijam privilégio administrativo.

## Objetivos Específicos

| # | Objetivo | Implementação |
|---|----------|--------------|
| 1 | Listar usuários de forma paginada | `Page<UserRes>` com `Pageable` |
| 2 | Permitir busca por nome e email | Query com `LIKE` case-insensitive |
| 3 | Proteger dados sensíveis | `UserRes` nunca expõe `password` |
| 4 | Restringir edições destrutivas | `@PreAuthorize isAdmin()` em PUT/DELETE |
| 5 | Permitir atualização de senha | `PATCH` com BCrypt para qualquer autenticado |
| 6 | Rastrear criação e modificação | Auditoria automática (created_by, updated_by) |
