# Auth — Regras de Negócio

> **Data de criação:** 2026-09-23

---

## RN-AUTH-001 — Senha gerada automaticamente no registro

Ao registrar um novo usuário, o sistema **não aceita senha fornecida pelo cliente**. Uma senha temporária de 10 caracteres é gerada pelo servidor usando `SecureRandom`. A senha satisfaz os seguintes critérios de complexidade:
- Ao menos 1 letra maiúscula
- Ao menos 1 letra minúscula
- Ao menos 1 dígito numérico
- Ao menos 1 caractere especial (`@$!%*?&#`)
- Comprimento total: 10 caracteres
- Os caracteres são embaralhados com algoritmo Fisher-Yates

## RN-AUTH-002 — Envio de senha por e-mail

Após o registro bem-sucedido, o sistema envia um e-mail HTML para o endereço cadastrado com:
- Saudação com o nome do usuário
- A senha temporária em texto claro
- Instrução para alterar a senha no primeiro acesso
- Remetente: `Suporte Fitoherb <noreply@fitoherb.com.br>`

## RN-AUTH-003 — Somente ADMIN pode registrar novos usuários

O endpoint `POST /auth/register` requer `@PreAuthorize("@authorizationService.isAdmin()")`. Um visitante anônimo ou usuário com `ROLE_USER` não pode criar contas.

## RN-AUTH-004 — E-mail único por conta

Se o e-mail informado no registro já estiver em uso, o sistema lança `ResourceAlreadyExistsException` com HTTP `409 Conflict`.

## RN-AUTH-005 — Token JWT expira em 2 horas

Tokens gerados têm validade de 2 horas a partir do momento da emissão, no fuso `-03:00 (BRT)`.

## RN-AUTH-006 — Refresh aceita token expirado (se assinatura válida)

O endpoint `POST /auth/refresh` usa `validateAndGetSubjectEvenIfExpired()`, que aceita tokens com a assinatura correta mesmo após expiração. Tokens com assinatura inválida (forjados) são rejeitados com `InvalidTokenException`.

## RN-AUTH-007 — Cookie rememberMe

- `rememberMe = true` → cookie `fitoherb_jwt` tem `MaxAge = 30 dias`
- `rememberMe = false` ou ausente → cookie de sessão (`MaxAge = -1`, expira ao fechar o browser)

## RN-AUTH-008 — Cookie HttpOnly para JWT

O cookie `fitoherb_jwt` é criado com `setHttpOnly(true)`, tornando-o inacessível ao JavaScript do frontend, protegendo contra ataques XSS.

## RN-AUTH-009 — Logout limpa cookies

`POST /auth/logout` redefine os cookies `fitoherb_jwt` e `fitoherb_user_email` com `MaxAge = 0`, efetivamente deletando-os do browser.

## RN-AUTH-010 — Falha de persistência não expõe detalhes internos

Se `userRepository.save()` lançar qualquer exceção, o sistema captura e relança como `DatabaseOperationException` com mensagem genérica, sem expor stack trace para o cliente.
