# Visão Geral dos Módulos — Fitoherb Backend v2

> **Data de criação:** 2026-09-23  
> **Versão:** 0.0.1-SNAPSHOT

---

## 1. Mapa de Módulos

```
fitoherb-backend-v2/
├── auth            (AuthController + AuthorizationService)
├── users           (UserController + UserService)
├── products        (ProductController + ProductService)
├── product-categories  (ProductCategoryController + ProductCategoryService)
├── suppliers       (SupplierController + SupplierService)
├── banners         (BannerController + BannerService)
├── infra/security  (SecurityFilter + SecurityConfigurations + AuditorAwareImpl)
├── infra/exceptions (RestExceptionHandler + RestErrorMessage)
└── services (auxiliares)
    ├── TokenService
    ├── MailService
    └── FileStorageService (LocalFileStorageService / GcsFileStorageService)
```

---

## 2. Tabela de Módulos

| Módulo | Package | Tipo | Responsabilidade Principal |
|--------|---------|------|--------------------------|
| **Auth** | `controllers.AuthController` | Controller + Service | Autenticação JWT, registro de usuários, refresh/logout de token |
| **Users** | `controllers.UserController` | Controller + Service | CRUD de usuários, busca paginada, atualização de senha |
| **Products** | `controllers.ProductController` | Controller + Service | CRUD de produtos com imagem multipart, galeria pública paginada |
| **Product Categories** | `controllers.ProductCategoryController` | Controller + Service | CRUD de categorias de produtos com imagem |
| **Suppliers** | `controllers.SupplierController` | Controller + Service | CRUD de fornecedores com imagem e flag de destaque |
| **Banners** | `controllers.BannerController` | Controller + Service | CRUD de banners da homepage com imagem e posicionamento |
| **Security (Infra)** | `infra.security` | Infraestrutura | Filtro JWT, configuração de SecurityFilterChain, CORS, BCrypt, Auditoria |
| **Exception Handler (Infra)** | `infra.exceptions` | Infraestrutura | Tratamento global de exceções com respostas padronizadas |
| **TokenService** | `services.TokenService` | Serviço Auxiliar | Geração, validação e refresh de tokens JWT via Auth0 |
| **MailService** | `services.MailService` | Serviço Auxiliar | Envio de e-mails HTML via JavaMailSender (Gmail SMTP) |
| **FileStorageService** | `services.FileStorageService` | Interface + 2 Impl. | Armazenamento de imagens (Local em dev, GCS em produção) |

---

## 3. Detalhamento por Módulo

### 3.1 Auth (`/auth`)

**Arquivos:**
- `controllers/AuthController.java`
- `services/AuthorizationService.java`
- `dtos/requests/LoginReq.java`, `RegisterReq.java`
- `dtos/responses/LoginRes.java`
- `mappers/AuthMapper.java`

**Responsabilidades:**
- `POST /auth/login` — autenticar usuário e retornar JWT + configurar cookies
- `POST /auth/register` — registrar novo usuário (restrito a admins), gerar senha aleatória e enviar por e-mail
- `POST /auth/refresh` — renovar token expirado via cookie ou header
- `POST /auth/logout` — limpar cookies de autenticação
- Implementa `UserDetailsService` do Spring Security para carregar usuário por email

**Integrações internas:** `TokenService`, `MailService`, `UserRepository`, `AuthMapper`

---

### 3.2 Users (`/users`)

**Arquivos:**
- `controllers/UserController.java`
- `services/UserService.java`
- `dtos/requests/UserReq.java`, `PasswordUpdateReq.java`
- `dtos/responses/UserRes.java`
- `mappers/UserMapper.java`
- `repositories/UserRepository.java`
- `entities/User.java`

**Responsabilidades:**
- Listar usuários com paginação, busca e ordenação
- Buscar usuário por e-mail
- Atualizar perfil de usuário (somente admin)
- Atualizar senha (qualquer usuário autenticado para a própria conta)
- Deletar usuário (somente admin)

**Controle de acesso:**
- `GET /users` e `GET /users/{email}` — qualquer usuário autenticado
- `PUT`, `DELETE /users/{email}` — somente `ROLE_ADMIN`
- `PATCH /users/update-password/{email}` — qualquer usuário autenticado

---

### 3.3 Products (`/products`)

**Arquivos:**
- `controllers/ProductController.java`
- `services/ProductService.java`
- `dtos/requests/ProductReq.java`
- `dtos/responses/ProductRes.java`
- `mappers/ProductMapper.java`
- `repositories/ProductRepository.java`
- `entities/Product.java`

**Responsabilidades:**
- CRUD completo de produtos com upload de imagem via multipart
- Listagem paginada para painel administrativo com filtros por categoria/fornecedor
- Galeria pública paginada (endpoint público sem autenticação)
- Geração automática de slug a partir do nome
- Armazenamento e remoção de imagens via `FileStorageService`
- Validação de conflito de slug e nome antes de criar/atualizar

**Campos especiais:**
- `flavours` — array de strings com sabores disponíveis
- `presentation` — array de strings com apresentações disponíveis (ex: 100g, 250g)
- `category` e `supplier` — FKs resolvidas por slug no request

---

### 3.4 Product Categories (`/product_categories`)

**Arquivos:**
- `controllers/ProductCategoryController.java`
- `services/ProductCategoryService.java`
- `dtos/requests/ProductCategoryReq.java`
- `dtos/responses/ProductCategoryRes.java`
- `mappers/ProductCategoryMapper.java`
- `repositories/ProductCategoryRepository.java`
- `entities/ProductCategory.java`

**Responsabilidades:**
- CRUD de categorias com imagem
- Listagem completa (sem paginação) para dropdowns/filtros — `GET /product_categories/get-all`
- Listagem paginada para administração — `GET /product_categories`
- Geração automática de slug via `@PrePersist/@PreUpdate`

---

### 3.5 Suppliers (`/suppliers`)

**Arquivos:**
- `controllers/SupplierController.java`
- `services/SupplierService.java`
- `dtos/requests/SupplierReq.java`
- `dtos/responses/SupplierRes.java`
- `mappers/SupplierMapper.java`
- `repositories/SupplierRepository.java`
- `entities/Supplier.java`

**Responsabilidades:**
- CRUD de fornecedores com imagem e flag de destaque (`isHighlighted`)
- Listagem completa para dropdowns — `GET /suppliers/get-all`
- Listagem paginada para administração — `GET /suppliers`
- Deleção em cascata opcional de produtos ao deletar fornecedor (`deleteProducts` param)
- Proteção contra deleção de fornecedores com produtos vinculados (sem flag)

---

### 3.6 Banners (`/banners`)

**Arquivos:**
- `controllers/BannerController.java`
- `services/BannerService.java`
- `dtos/requests/BannerReq.java`
- `dtos/responses/BannerRes.java`
- `mappers/BannerMapper.java`
- `repositories/BannerRepository.java`
- `entities/Banner.java`

**Responsabilidades:**
- CRUD de banners da homepage com imagem
- Controle de posição e status (ativo/inativo) dos banners
- Listagem pública de banners ativos, ordenados por posição — `GET /banners/active`
- Listagem paginada para administração — `GET /banners`

---

### 3.7 Infra — Security

**Arquivos:**
- `infra/security/SecurityConfigurations.java` — `@Configuration` com `SecurityFilterChain`
- `infra/security/SecurityFilter.java` — `OncePerRequestFilter` que valida JWT
- `infra/security/AuditorAwareImpl.java` — `AuditorAware<String>` para campos `@CreatedBy`/`@LastModifiedBy`

**Responsabilidades:**
- Definir política de sessão stateless
- Configurar CORS com origens autorizadas
- Injetar `SecurityFilter` antes do `UsernamePasswordAuthenticationFilter`
- Prover `BCryptPasswordEncoder` como bean
- Fornecer `AuthenticationManager` como bean
- Alimentar campos de auditoria com o email do usuário autenticado

---

### 3.8 Infra — Exception Handler

**Arquivos:**
- `infra/exceptions/RestExceptionHandler.java` — `@RestControllerAdvice`
- `infra/exceptions/RestErrorMessage.java` — DTO de erro simples (`status`, `message`)
- `infra/exceptions/RestValidationErrorMessage.java` — DTO de erro com mapa de campos (`status`, `message`, `errors`)

**Responsabilidades:**
- Capturar exceções de domínio e HTTP globalmente
- Formatar respostas de erro em JSON padronizado
- Logar erros inesperados

---

### 3.9 TokenService

**Arquivo:** `services/TokenService.java`

| Método | Descrição |
|--------|-----------|
| `generateToken(User)` | Gera JWT assinado com HMAC256, expira em 2h |
| `validateToken(String)` | Valida assinatura e expiração, retorna email (subject) |
| `validateAndGetSubjectEvenIfExpired(String)` | Valida assinatura mas aceita token expirado (para refresh) |

---

### 3.10 MailService

**Arquivo:** `services/MailService.java`

- Envia e-mail HTML via `JavaMailSender` (Gmail SMTP)
- Template HTML com branding Fitoherb (fundo verde `#2e7d32`)
- Usado pelo `AuthorizationService` para envio de senha temporária no cadastro

---

### 3.11 FileStorageService

**Arquivos:**
- `services/FileStorageService.java` — interface com métodos `store*` e `delete*`
- `services/LocalFileStorageService.java` — salva em disco local (dev)
- `services/GcsFileStorageService.java` — salva no Google Cloud Storage (prod)

**Diretórios locais (exemplo Windows):**
```
C:\Users\PC\Documents\FitoherbImages\Suppliers\
C:\Users\PC\Documents\FitoherbImages\Products\
C:\Users\PC\Documents\FitoherbImages\Categories\
C:\Users\PC\Documents\FitoherbImages\Banners\
```

**Nota:** Imagens são redimensionadas/comprimidas via `Thumbnailator` antes de salvar.
