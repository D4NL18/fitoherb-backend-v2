# Objetivos do Projeto — Fitoherb Backend v2

> **Data de criação:** 2026-09-23  
> **Empresa:** Fitoherb Nordeste Distribuidora de Suplementos Naturais  
> **Localização:** Lauro de Freitas, Bahia — Brasil

---

## 1. Contexto do Negócio

A **Fitoherb Nordeste** é uma distribuidora de suplementos naturais e fitoterápicos localizada em Lauro de Freitas/BA. A empresa atua no mercado B2B e B2C, comercializando produtos de diversas marcas (fornecedores) organizados em categorias temáticas.

O **Fitoherb Backend v2** é a espinha dorsal tecnológica da plataforma digital da empresa, responsável por fornecer dados e operações seguras para:

- O **site público** da distribuidora (vitrine de produtos para clientes)
- O **painel administrativo** (gestão interna de catálogo, fornecedores e usuários)

---

## 2. Objetivo Geral

Prover uma **API RESTful robusta, segura e escalável** que gerencie todo o ciclo de vida do catálogo de produtos da Fitoherb Nordeste, incluindo autenticação de usuários, gerenciamento de conteúdo (produtos, categorias, fornecedores, banners) e armazenamento de imagens.

---

## 3. Objetivos Técnicos

### 3.1 Segurança
- Autenticação **stateless** com JWT (sem sessão no servidor)
- Senhas armazenadas com hash **BCrypt**
- Controle de acesso baseado em **roles** (`ROLE_ADMIN`, `ROLE_USER`)
- Proteção CSRF desabilitada (padrão para APIs REST com JWT)
- CORS configurado para domínios autorizados da Fitoherb

### 3.2 Qualidade e Manutenibilidade
- Separação clara de responsabilidades em camadas (Controller → Service → Repository → Entity)
- Uso de **DTOs** para nunca expor entidades JPA diretamente
- **MapStruct** para mapeamento sem boilerplate e com segurança de tipos
- **Lombok** para redução de código repetitivo
- Handler de exceções global com respostas padronizadas
- Campos de **auditoria automática** (criado por, criado em, modificado por, modificado em)

### 3.3 Escalabilidade e Flexibilidade
- **Strategy Pattern** para armazenamento de arquivos (Local em dev, GCS em produção)
- Containerização via **Docker** (multi-stage build) para deploy em qualquer ambiente
- Paginação em todos os endpoints de listagem administrativa
- Filtros e ordenação dinâmica nos endpoints de busca

### 3.4 Documentação e Observabilidade
- Documentação interativa via **Swagger UI / OpenAPI 3**
- Logs estruturados com **Slf4j/Logback**
- Endpoint de healthcheck (`GET /health`)

### 3.5 Comunicação
- Envio de e-mails transacionais via **Spring Boot Mail** (Gmail SMTP)
  - E-mail de boas-vindas com senha temporária no cadastro de usuários

---

## 4. Público-Alvo

| Perfil | Acesso | Uso Principal |
|--------|--------|--------------|
| **Administrador** (`ROLE_ADMIN`) | Total — leitura e escrita em todos os módulos | Gerenciar produtos, categorias, fornecedores, banners e usuários pelo painel |
| **Usuário autenticado** (`ROLE_USER`) | Leitura — seu próprio perfil e catálogo | Consultar catálogo e gerenciar própria conta |
| **Visitante público** | Somente endpoints públicos | Visualizar galeria de produtos, categorias e fornecedores no site |
| **Integrações** | Conforme role do token | Frontend Angular, ferramentas de automação |

---

## 5. Escopo v2 vs v1

A versão 2 representa uma reescrita e modernização do backend original:

| Aspecto | v1 | v2 |
|---------|----|----|
| **Framework** | Spring Boot (versão anterior) | Spring Boot 4.0.5 |
| **Segurança** | Básica | JWT stateless + roles + method security |
| **Armazenamento** | Local apenas | Local + GCS (Strategy Pattern) |
| **Documentação** | Manual/inexistente | OpenAPI 3 (Swagger) automático |
| **Auditoria** | Sem auditoria | Auditoria completa em todas as entidades |
| **Mapeamento** | Manual | MapStruct (gerado em compilação) |
| **Validação** | Básica | Bean Validation completo com mensagens customizadas |
| **Tratamento de erros** | Não padronizado | Global exception handler com DTOs padronizados |
| **Banners** | Não existia | Módulo completo de banners com imagem |
| **Emails** | Não existia | Spring Mail com template HTML |
| **Refresh token** | Não existia | Endpoint `/auth/refresh` + cookies HttpOnly |

---

## 6. Metas de Segurança e Qualidade

### Segurança
- [x] Nenhuma senha armazenada em texto plano
- [x] Tokens JWT com expiração de 2 horas
- [x] HTTPS obrigatório em produção (configurado no proxy/balanceador)
- [x] CORS restrito a domínios Fitoherb autorizados
- [x] Senhas temporárias geradas com `SecureRandom` (criptograficamente seguras)
- [x] Tokens em cookies HttpOnly (protegidos contra XSS)
- [ ] Rate limiting (previsto para versão futura)
- [ ] Autenticação de dois fatores (previsto para versão futura)

### Qualidade de Código
- [x] Análise estática com **SonarQube** (recomendado)
- [x] Testes unitários com JUnit 5 (infraestrutura preparada)
- [x] Build reproducível via Gradle wrapper
- [x] Imagem Docker leve baseada em Alpine Linux

---

## 7. Variáveis de Ambiente Requeridas

| Variável | Descrição | Obrigatória |
|----------|-----------|-------------|
| `PostgresUsername` | Usuário do banco de dados PostgreSQL | ✅ Sempre |
| `PostgresPassword` | Senha do PostgreSQL (também usada como JWT secret) | ✅ Sempre |
| `mailSenderUsername` | E-mail remetente (Gmail SMTP) | ✅ Sempre |
| `mailSenderPassword` | Senha de aplicativo Gmail | ✅ Sempre |
| `GOOGLE_APPLICATION_CREDENTIALS` | Caminho para JSON de credenciais GCS | ✅ Produção |

---

## 8. Roadmap e Itens Futuros

| Funcionalidade | Prioridade | Status |
|---------------|-----------|--------|
| Rate limiting nos endpoints de autenticação | Alta | Planejado |
| Testes de integração abrangentes | Alta | Em progresso |
| Autenticação OAuth2 (Google/Social) | Média | Planejado |
| Webhook para notificações de estoque | Média | Planejado |
| Cache com Redis | Média | Planejado |
| 2FA (Two-Factor Authentication) | Baixa | Planejado |
| Exportação de relatórios (PDF/Excel) | Baixa | Planejado |
