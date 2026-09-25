# Users — Regras de Negócio

> **Data de criação:** 2026-09-23

---

## RN-USERS-001 — Listagem e busca exigem autenticação

`GET /users` e `GET /users/{email}` requerem qualquer usuário autenticado (qualquer role). Visitantes não autenticados recebem `401 Unauthorized`.

## RN-USERS-002 — Atualização de perfil restrita a Admin

`PUT /users/{email}` requer `ROLE_ADMIN`. Usuários com `ROLE_USER` recebem `403 Forbidden`.

## RN-USERS-003 — Deleção restrita a Admin

`DELETE /users/{email}` requer `ROLE_ADMIN`. A deleção é permanente e irreversível.

## RN-USERS-004 — Atualização de senha não exige privilégio de admin

`PATCH /users/update-password/{email}` está disponível para qualquer usuário autenticado. Qualquer usuário pode alterar a senha de qualquer conta por email — a limitação de "somente a própria senha" deve ser implementada na camada de serviço (verificação de identidade).

## RN-USERS-005 — Senha armazenada com BCrypt

Ao atualizar a senha, o `UserService` usa `PasswordEncoder.encode()` antes de persistir. Nunca armazena texto plano.

## RN-USERS-006 — Paginação com tamanho fixo de 10

A listagem `GET /users` retorna 10 itens por página (tamanho padrão do Spring Data Pageable).

## RN-USERS-007 — Busca textual por nome e e-mail

O parâmetro `search` filtra resultados que contenham o termo no campo `name` ou `email` (case-insensitive via `LOWER()`).

## RN-USERS-008 — Ordenação dinâmica

Os campos permitidos para `sortField` são: `name`, `email`, `role`, `createdAt`. Qualquer outro valor pode causar erro. A direção `direction` aceita `ASC` ou `DESC`.

## RN-USERS-009 — Usuário não encontrado retorna 404

Se o e-mail informado não corresponder a nenhum registro, o sistema lança `ResourceNotFoundException` com HTTP `404 Not Found`.

## RN-USERS-010 — A senha não é retornada em nenhum response

O DTO `UserRes` expõe apenas `email`, `name`, `role` e `createdAt`. O campo `password` (hash) nunca é incluído em qualquer response da API.
