# Auth — Visão Geral do Módulo

> **Data de criação:** 2026-09-23  
> **Módulo:** Autenticação e Gerenciamento de Identidade (IAM)  
> **Controller:** `AuthController`  
> **Service:** `AuthorizationService`

---

## Descrição

O módulo **Auth** é responsável por toda a gestão de identidade e acesso da plataforma Fitoherb. Ele provê mecanismos de autenticação stateless baseados em **JWT (JSON Web Tokens)**, registro seguro de usuários e renovação de sessão.

## Funcionalidades

| Funcionalidade | Endpoint | Descrição |
|---------------|----------|-----------|
| Login | `POST /auth/login` | Autentica credenciais e emite JWT |
| Registro | `POST /auth/register` | Cria conta com senha aleatória e envia por e-mail |
| Refresh de Token | `POST /auth/refresh` | Renova JWT sem re-autenticação completa |
| Logout | `POST /auth/logout` | Limpa cookies de sessão |

## Dependências

```
AuthController
    └── AuthorizationService
            ├── TokenService       (geração/validação JWT)
            ├── MailService        (envio de senha temporária)
            ├── UserRepository     (persistência)
            └── AuthMapper         (Entity → DTO)
```

## Tecnologias

- **Auth0 java-jwt 4.4.0** — geração e validação de tokens HMAC256
- **BCryptPasswordEncoder** — hashing de senhas
- **JavaMailSender** — envio de e-mails HTML
- **Spring Security `UserDetailsService`** — integração com o filtro de segurança
- **Cookies HttpOnly** — armazenamento seguro do token no browser
