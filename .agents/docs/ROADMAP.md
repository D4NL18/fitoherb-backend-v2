# Roadmap do Projeto — Fitoherb Backend v2 🗺️

> **Localização Mandatória:** `.agents/docs/ROADMAP.md`  
> **Responsável:** Product Owner (`product_owner.md`) / `agile-coach`  
> **Empresa:** Fitoherb Nordeste Distribuidora de Suplementos Naturais  
> **Stack:** Java 21 · Spring Boot 3 / 4.x · PostgreSQL · Hibernate/JPA · MapStruct · Flyway

---

## 1. Visão do Produto

O **Fitoherb Backend v2** é a espinha dorsal tecnológica da plataforma da **Fitoherb Nordeste**, fornecendo uma API RESTful corporativa, robusta, segura e escalável para gerenciar todo o ciclo de vida do catálogo de fitoterápicos e suplementos, servindo tanto o site público quanto o painel administrativo.

---

## 2. Épicos e User Stories (Backlog & Status)

### Épico 1: Fundação, Segurança e Acesso
- [x] **US-01 — Autenticação Stateless JWT:** Login seguro com senhas em BCrypt, roles (`ROLE_ADMIN`, `ROLE_USER`) e cookies HttpOnly.
- [x] **US-02 — Refresh Token e Sessão:** Endpoint `/auth/refresh` com renovação de sessão sem interrupção de fluxo.
- [x] **US-03 — Gestão de Usuários:** CRUD de operadores com geração de senha temporária via `SecureRandom` e envio de e-mail de boas-vindas via Spring Mail.

### Épico 2: Gestão de Catálogo e Mídia
- [x] **US-04 — Catálogo de Produtos e Slugs Dinâmicos:** CRUD de produtos com campos otimizados, sabores, apresentações e geração de slugs amigáveis.
- [x] **US-05 — Categorias e Fornecedores:** Estruturas relacionais com integridade referencial forte (`product_categories` e `suppliers`).
- [x] **US-06 — Gestão de Banners:** Controle de banners promocionais da Home com status ativo/inativo e ordenação.
- [x] **US-07 — Upload e Armazenamento Híbrido:** Strategy Pattern para storage de arquivos (Local em dev, GCS em produção).

### Épico 3: Robustez de Busca, Auditoria e LGPD
- [x] **US-08 — Busca Textual Otimizada sem Acentos:** Busca resiliente no catálogo usando funções de banco e CriteriaBuilder.
- [x] **US-09 — Auditoria Automática de Entidades:** Rastreabilidade completa via JPA Auditing (`created_at`, `updated_at`, `created_by`, `updated_by`).
- [ ] **US-10 — Rate Limiting e Proteção de Borda:** Implementação de controle de taxa para endpoints sensíveis de login e cadastro. `[PARALLEL]`
- [ ] **US-11 — Relatório de Impacto à Privacidade (LGPD):** Auditoria contínua de retenção e minimização de dados de clientes e parceiros. `[PARALLEL]`

---

## 3. Histórico de Entregas Recentes

- **fix/unaccent-search:** Otimização de busca textual com tratamento nativo de acentuação e refatoração de manutenibilidade no Sonar.
- **cors_qa_fix:** Adequação de headers de CORS para suporte dual em ambientes locais e QA.
- **feature/auth-refresh:** Implementação de refresh tokens em cookies seguros.
