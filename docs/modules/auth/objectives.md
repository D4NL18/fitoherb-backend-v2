# Auth — Objetivos do Módulo

> **Data de criação:** 2026-09-23

---

## Objetivo Principal

Prover um mecanismo de **autenticação stateless seguro** para a plataforma Fitoherb, eliminando a necessidade de sessões no servidor e garantindo que todas as requisições autenticadas sejam verificáveis de forma independente.

## Objetivos Específicos

| # | Objetivo | Como é Alcançado |
|---|----------|-----------------|
| 1 | Autenticar usuários com segurança | JWT HMAC256 com expiração de 2h |
| 2 | Proteger senhas dos usuários | BCrypt hashing — nunca armazenadas em texto plano |
| 3 | Gerar senhas iniciais seguras | SecureRandom com critérios de complexidade |
| 4 | Notificar novos usuários | E-mail HTML com credenciais iniciais via JavaMailSender |
| 5 | Permitir renovação de sessão | Refresh token via cookie HttpOnly sem re-login |
| 6 | Restringir criação de contas | Apenas administradores podem registrar usuários |
| 7 | Proteger contra XSS | Cookie JWT com HttpOnly flag |
| 8 | Suportar "lembrar de mim" | Cookie de longa duração (30 dias) ou de sessão |
| 9 | Implementar logout seguro | Invalidação de cookies via MaxAge=0 |

## Métricas de Sucesso

- Nenhuma senha armazenada em texto plano no banco de dados
- Todos os tokens expiram em no máximo 2 horas (sem rememberMe)
- Envio de e-mail bem-sucedido em 100% dos novos registros
- Zero exposição de stack traces nas respostas de erro
