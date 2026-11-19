# Contrato de API — API Keys administrativas

Permite que clientes programáticos (ex.: Claude/agentes em outros projetos) executem ações de admin sem login interativo.

## Uso pelo cliente
Enviar o header em qualquer endpoint protegido por `isAdmin()`:

```
X-API-Key: fh_<segredo>
```

- Chave válida → a requisição é tratada como `ROLE_ADMIN` (auditoria registra `api-key:<nome>`).
- Chave inválida/revogada → `401` imediato.
- Sem o header → fluxo normal (JWT por cookie/Bearer).
- Cabeçalho `X-API-Key` nunca deve ser logado nem commitado; guardar como secret no projeto cliente.

## Gestão (somente admin logado via JWT — API key NÃO acessa estes endpoints)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api-keys` | Cria chave. Body `{ "name": "claude-projeto-x" }`. `201` com `{ key: {...}, apiKey: "fh_..." }` — o segredo aparece **uma única vez**. |
| GET | `/api-keys` | Lista metadados (`id`, `name`, `keyPrefix`, `active`, `lastUsedAt`, `revokedAt`, ...). Nunca devolve o segredo. |
| DELETE | `/api-keys/{id}` | Revoga imediatamente. `204`; `404` se não existir. |

## Segurança (regras)
- Segredo com 256 bits (`SecureRandom`); no banco fica apenas o SHA-256 (`api_keys.key_hash`, único) e o prefixo de 8 chars para identificação.
- Chave não pode criar/listar/revogar chaves (anti-escalonamento); a rota `/api-keys` ignora o header.
- `lastUsedAt` é atualizado no máximo 1x/minuto por chave.
- Escopo: uma chave equivale a um admin completo (inclui `/auth/register` e `/users`). Revogar chaves não utilizadas.

## Tabela
`api_keys(id uuid pk, name, key_hash char(64) unique, key_prefix, last_used_at, revoked_at, created_at, created_by)` — criada por `ddl-auto=update`.
