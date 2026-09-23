# Modelo de Domínio — Fitoherb Backend v2

> **Data de criação:** 2026-09-23  
> **Domínio:** Distribuidora de Suplementos Naturais e Fitoterápicos

---

## 1. Visão Geral do Domínio

A Fitoherb Nordeste é uma **distribuidora** que comercializa produtos de suplementos naturais de diferentes **fornecedores** (fabricantes), organizados em **categorias** temáticas. A plataforma digital expõe um **catálogo** (galeria de produtos) para o público e um **painel administrativo** para gestão interna. Banners promocionais são exibidos na homepage.

```
FITOHERB NORDESTE (distribuidora)
    │
    ├── gerencia → Fornecedores (Suppliers)
    │       └── fabricam → Produtos (Products)
    │
    ├── organiza → Categorias (ProductCategories)
    │       └── classificam → Produtos (Products)
    │
    ├── exibe → Banners
    │
    └── administrada por → Usuários (Users) com role ADMIN
```

---

## 2. Entidades do Domínio

### 2.1 User (Usuário)

Representa uma conta de acesso à plataforma Fitoherb.

```
User {
  id          : UUID           — identificador único imutável
  email       : String         — e-mail de login, único, imutável na prática
  name        : String         — nome completo do usuário
  password    : String         — hash BCrypt da senha
  role        : UserRole       — papel de acesso: ADMIN | USER
  createdAt   : LocalDateTime  — data/hora de criação (automático)
  updatedAt   : LocalDateTime  — data/hora da última modificação (automático)
  createdBy   : String         — e-mail do criador (automático)
  updatedBy   : String         — e-mail do último modificador (automático)
}
```

**Comportamento:**
- Implementa `UserDetails` do Spring Security
- `getUsername()` retorna `email`
- `getAuthorities()` → ADMIN recebe `ROLE_ADMIN` + `ROLE_USER`; USER recebe apenas `ROLE_USER`
- Todas as flags de conta (`isEnabled`, `isAccountNonExpired`, etc.) retornam `true`

---

### 2.2 ProductCategory (Categoria de Produto)

Agrupa produtos por tema ou tipo.

```
ProductCategory {
  id          : UUID           — identificador único imutável
  name        : String         — nome único da categoria (ex: "Chás e Infusões")
  slug        : String         — URL-friendly, único, gerado do name (@PrePersist)
  imagePath   : String         — caminho/URL da imagem de capa
  createdAt   : LocalDateTime
  updatedAt   : LocalDateTime
  createdBy   : String
  updatedBy   : String
}
```

**Comportamento:**
- `@PrePersist` e `@PreUpdate` geram o slug automaticamente
- Nome e slug têm UNIQUE constraint no banco
- Deleção bloqueada se houver produtos vinculados (FK)

---

### 2.3 Supplier (Fornecedor)

Representa o fabricante ou distribuidor de origem de um produto.

```
Supplier {
  id            : UUID           — identificador único imutável
  name          : String         — nome único do fornecedor (ex: "Fitoherb Natural")
  slug          : String         — URL-friendly, único, gerado do name (@PrePersist)
  imagePath     : String         — caminho/URL do logotipo
  isHighlighted : Boolean        — se deve ser exibido em destaque no site (nullable)
  createdAt     : LocalDateTime
  updatedAt     : LocalDateTime
  createdBy     : String
  updatedBy     : String
}
```

**Comportamento:**
- `@PrePersist` e `@PreUpdate` geram o slug automaticamente
- `isHighlighted` é nullable: `null` = não definido, `false` = explicitamente não destacado
- Deleção bloqueada por FK; deleção em cascata via `deleteProducts=true`

---

### 2.4 Product (Produto)

O item central do catálogo Fitoherb.

```
Product {
  id           : UUID               — identificador único imutável
  name         : String             — nome do produto (ex: "Chá de Camomila Orgânico")
  slug         : String             — URL-friendly único, gerado do name (@PrePersist)
  imagePath    : String             — caminho/URL da imagem principal
  description  : String (TEXT)      — descrição detalhada dos benefícios e características
  flavours     : List<String>       — sabores disponíveis (ex: ["Chocolate", "Morango"])
  presentation : List<String>       — apresentações disponíveis (ex: ["100g", "250g", "500g"])
  category     : ProductCategory    — categoria do produto (MANY_TO_ONE, LAZY)
  supplier     : Supplier           — fornecedor do produto (MANY_TO_ONE, LAZY)
  createdAt    : LocalDateTime
  updatedAt    : LocalDateTime
  createdBy    : String
  updatedBy    : String
}
```

**Comportamento:**
- `@PrePersist` e `@PreUpdate` geram o slug automaticamente
- `description` pode ser `null` (produto sem descrição)
- `flavours` e `presentation` são arrays PostgreSQL (`TEXT[]`)
- Um produto pertence a exatamente 1 categoria e 1 fornecedor

---

### 2.5 Banner

Elemento visual para a homepage do site.

```
Banner {
  id         : UUID           — identificador único
  title      : String         — título do banner (obrigatório)
  imagePath  : String         — caminho/URL da imagem (obrigatório)
  isActive   : boolean        — visibilidade no site (padrão: true)
  position   : int            — ordem de exibição (padrão: 0, menor = primeiro)
  createdAt  : LocalDateTime
  updatedAt  : LocalDateTime
  createdBy  : String
  updatedBy  : String
}
```

**Comportamento:**
- Não possui slug (identificado por UUID)
- Apenas banners com `isActive = true` aparecem no endpoint público
- `position` determina a ordem crescente de exibição

---

## 3. Enums

### UserRole

```java
public enum UserRole {
    ADMIN,  // acesso total — ROLE_ADMIN + ROLE_USER
    USER    // acesso limitado — ROLE_USER
}
```

---

## 4. Relações entre Entidades

```
ProductCategory ──< Product >── Supplier
     (1)          (N)   (N)       (1)
```

| Relação | Cardinalidade | Fetch | Descrição |
|---------|--------------|-------|-----------|
| Product → ProductCategory | MANY_TO_ONE | LAZY | Um produto pertence a uma categoria |
| Product → Supplier | MANY_TO_ONE | LAZY | Um produto é de um fornecedor |
| ProductCategory → [Products] | ONE_TO_MANY | — | Uma categoria contém muitos produtos |
| Supplier → [Products] | ONE_TO_MANY | — | Um fornecedor tem muitos produtos |
| User | Independente | — | Sem relações com outras entidades de domínio |
| Banner | Independente | — | Sem relações com outras entidades de domínio |

---

## 5. Padrões de Identificação

| Entidade | Identificador Primário | Identificador Público (URL) |
|----------|----------------------|---------------------------|
| User | UUID (`id`) | E-mail (`email`) |
| ProductCategory | UUID (`id`) | Slug (`slug`) |
| Supplier | UUID (`id`) | Slug (`slug`) |
| Product | UUID (`id`) | Slug (`slug`) |
| Banner | UUID (`id`) | UUID (`id`) |

---

## 6. Campos de Auditoria (Todos os Entities)

Todos os 5 entities possuem os mesmos 4 campos de auditoria automáticos:

| Campo | Tipo | Automático | Mutável |
|-------|------|-----------|---------|
| `created_at` | TIMESTAMP | ✅ @CreatedDate | ❌ |
| `updated_at` | TIMESTAMP | ✅ @LastModifiedDate | ✅ |
| `created_by` | VARCHAR | ✅ @CreatedBy (email) | ❌ |
| `updated_by` | VARCHAR | ✅ @LastModifiedBy (email) | ✅ |

---

## 7. Serviços de Domínio

| Serviço | Tipo | Responsabilidade |
|---------|------|-----------------|
| `AuthorizationService` | Service + UserDetailsService | Autenticação, registro, refresh de token, helpers de autorização |
| `UserService` | Service | CRUD de usuários, paginação, atualização de senha |
| `ProductService` | Service | CRUD de produtos, galeria, filtros |
| `ProductCategoryService` | Service | CRUD de categorias |
| `SupplierService` | Service | CRUD de fornecedores, deleção em cascata |
| `BannerService` | Service | CRUD de banners, listagem de ativos |
| `TokenService` | Serviço Auxiliar | Geração/validação JWT (Auth0 HMAC256) |
| `MailService` | Serviço Auxiliar | Envio de e-mails HTML (JavaMailSender) |
| `FileStorageService` | Interface (Strategy) | Armazenamento de imagens (Local / GCS) |

---

## 8. DTOs (Data Transfer Objects)

### Requests (entrada)

| DTO | Usado em | Campos principais |
|-----|----------|------------------|
| `LoginReq` | POST /auth/login | email, password, rememberMe |
| `RegisterReq` | POST /auth/register | email, name, role |
| `UserReq` | PUT /users/{email} | name, role |
| `PasswordUpdateReq` | PATCH /users/update-password | password |
| `ProductReq` | POST/PUT /products | name, description, categorySlug, supplierSlug, flavours[], presentation[] |
| `ProductCategoryReq` | POST/PUT /product_categories | name |
| `SupplierReq` | POST/PUT /suppliers | name, isHighlighted |
| `BannerReq` | POST/PUT /banners | title, isActive, position |
| `MailReq` | Interno (MailService) | email, subject, message |

### Responses (saída)

| DTO | Retornado em | Campos |
|-----|-------------|--------|
| `LoginRes` | POST /auth/login | token |
| `UserRes` | GET /users/* | email, name, role, createdAt |
| `ProductRes` | GET /products/* | name, imageUrl, description, flavours, presentation, slug, category, supplier, createdAt |
| `ProductCategoryRes` | GET /product_categories/* | name, slug, imageUrl |
| `SupplierRes` | GET /suppliers/* | name, slug, imageUrl, isHighlighted |
| `BannerRes` | GET /banners/* | id, title, imagePath, imageUrl, isActive, position, createdAt, updatedAt |
