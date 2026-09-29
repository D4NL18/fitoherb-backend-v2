# Users — Visão Geral do Módulo

> **Data de criação:** 2026-09-23  
> **Controller:** `UserController`  
> **Service:** `UserService`

---

## Descrição

O módulo **Users** gerencia o ciclo de vida completo das contas de usuário da plataforma Fitoherb. Oferece operações de leitura, atualização de perfil e atualização de senha, com controles de acesso baseados em roles.

## Funcionalidades

| Funcionalidade | Endpoint | Nível de Acesso |
|---------------|----------|----------------|
| Listar usuários (paginado) | `GET /users` | Autenticado |
| Buscar por e-mail | `GET /users/{email}` | Autenticado |
| Atualizar perfil | `PUT /users/{email}` | Admin |
| Atualizar senha | `PATCH /users/update-password/{email}` | Autenticado |
| Deletar usuário | `DELETE /users/{email}` | Admin |

## Dependências

```
UserController
    └── UserService
            ├── UserRepository     (consultas/persistência)
            ├── UserMapper         (Entity → UserRes)
            └── PasswordEncoder    (BCrypt para PATCH senha)
```

## DTOs

| DTO | Uso | Campos |
|-----|-----|--------|
| `UserReq` | Request de atualização | `name`, `role` |
| `PasswordUpdateReq` | Atualização de senha | `password` |
| `UserRes` | Resposta de perfil | `email`, `name`, `role`, `createdAt` |
