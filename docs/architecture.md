# Arquitetura Técnica — Fitoherb Backend v2

> **Data de criação:** 2026-09-23  
> **Versão da API:** 0.0.1-SNAPSHOT  
> **Stack:** Java 21 · Spring Boot 4.0.5 · PostgreSQL · JWT (Auth0)

---

## 1. Visão Geral

O Fitoherb Backend v2 é uma **API RESTful stateless** construída sobre Spring Boot 4. Segue uma arquitetura em camadas clássica (Layered Architecture), complementada por padrões modernos de segurança JWT e armazenamento de arquivos com estratégia intercambiável (Strategy Pattern).

---

## 2. Diagrama de Camadas

```
┌──────────────────────────────────────────────────────────────────┐
│                        CLIENTES (HTTP)                           │
│      Angular SPA · Swagger UI · Ferramentas REST (Postman)       │
└──────────────────────┬───────────────────────────────────────────┘
                       │ HTTP Request
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                  CAMADA DE SEGURANÇA (Filtros)                   │
│                                                                  │
│  SecurityFilter ──► TokenService.validateToken()                 │
│       │                                                          │
│       ▼ SecurityContextHolder.setAuthentication()                │
│  SecurityConfigurations (SecurityFilterChain · CORS · BCrypt)    │
└──────────────────────┬───────────────────────────────────────────┘
                       │ Requisição autorizada
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                  CAMADA DE CONTROLLERS (@RestController)          │
│                                                                  │
│  AuthController · UserController · ProductController             │
│  ProductCategoryController · SupplierController · BannerController│
│  HealthController · DataMigrationController · EmailController    │
│                                                                  │
│  ► Recebe DTOs de Request (@RequestBody / @RequestPart)          │
│  ► Aplica @Valid (Bean Validation)                               │
│  ► Aplica @PreAuthorize (Method Security)                        │
│  ► Delega lógica de negócio para a camada de Service             │
│  ► Retorna ResponseEntity<DTO de Response>                       │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                  CAMADA DE SERVICES (@Service)                   │
│                                                                  │
│  AuthorizationService · UserService · ProductService             │
│  ProductCategoryService · SupplierService · BannerService        │
│  TokenService · MailService · FileStorageService (interface)     │
│    ├── GcsFileStorageService (Google Cloud Storage)              │
│    └── LocalFileStorageService (armazenamento local/dev)         │
│                                                                  │
│  ► Contém toda a lógica de negócio                               │
│  ► Orquestra Repositories, Mappers e serviços auxiliares         │
│  ► Lança exceções de domínio tipadas                             │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│               CAMADA DE MAPPERS (MapStruct)                      │
│                                                                  │
│  AuthMapper · UserMapper · ProductMapper                         │
│  ProductCategoryMapper · SupplierMapper · BannerMapper           │
│                                                                  │
│  ► Converte Entity ↔ DTO (Request/Response)                      │
│  ► Gerado em tempo de compilação (sem overhead de reflexão)      │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│               CAMADA DE REPOSITORIES (Spring Data JPA)           │
│                                                                  │
│  UserRepository · ProductRepository · ProductCategoryRepository  │
│  SupplierRepository · BannerRepository                           │
│                                                                  │
│  ► Interfaces que estendem JpaRepository                         │
│  ► Queries customizadas via @Query (JPQL/SQL nativo)             │
│  ► Suporte a paginação via Pageable                              │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                    CAMADA DE ENTITIES (JPA/Hibernate)            │
│                                                                  │
│  User · Product · ProductCategory · Supplier · Banner            │
│                                                                  │
│  ► @Entity mapeadas para tabelas PostgreSQL                      │
│  ► Auditoria automática via @EntityListeners(AuditingEntityListener)│
│  ► @PrePersist/@PreUpdate para geração automática de slug        │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                   BANCO DE DADOS (PostgreSQL)                    │
│                                                                  │
│  users · products · product_categories · suppliers · banners     │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. Padrões e Convenções Adotados

### 3.1 DTO Pattern

Todas as trocas de dados entre cliente e API são feitas exclusivamente através de DTOs:

| Tipo | Descrição | Exemplos |
|------|-----------|---------|
| **Request DTO** (`*Req`) | Dados recebidos do cliente, com validações Bean Validation | `LoginReq`, `ProductReq`, `RegisterReq` |
| **Response DTO** (`*Res`) | Dados retornados ao cliente, nunca expõe entidades JPA diretamente | `UserRes`, `ProductRes`, `BannerRes` |

### 3.2 MapStruct (Mapper Layer)

- Mapeamento Entity↔DTO realizado por interfaces anotadas com `@Mapper`
- Código gerado em tempo de compilação pelo annotation processor
- Elimina boilerplate de mapeamento manual e garante segurança de tipos

### 3.3 Exception Handler Global

Localizado em `infra/exceptions/RestExceptionHandler.java` (`@RestControllerAdvice`):

| Exceção de Domínio | HTTP Status | Classe de Resposta |
|--------------------|-------------|-------------------|
| `ResourceNotFoundException` | 404 NOT_FOUND | `RestErrorMessage` |
| `ResourceAlreadyExistsException` | 409 CONFLICT | `RestErrorMessage` |
| `DatabaseOperationException` | 500 INTERNAL_SERVER_ERROR | `RestErrorMessage` |
| `InvalidTokenException` | 401 UNAUTHORIZED | `RestErrorMessage` |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | `RestValidationErrorMessage` |
| `AccessDeniedException` | 403 FORBIDDEN | `RestErrorMessage` |

### 3.4 Security Filter Chain

```
Requisição HTTP
      │
      ▼
SecurityFilter.doFilterInternal()
  ├── Extrai token do header "Authorization: Bearer <token>"
  │   ou do cookie "fitoherb_jwt"
  ├── tokenService.validateToken(token)  →  email do usuário
  ├── userRepository.findByEmail(email)  →  UserDetails
  ├── UsernamePasswordAuthenticationToken.setAuthenticated(true)
  └── SecurityContextHolder.getContext().setAuthentication(auth)
      │
      ▼
  Controller Method
  └── @PreAuthorize("@authorizationService.isAdmin()")
      ou @PreAuthorize("@authorizationService.isAuthenticated()")
```

### 3.5 JPA Auditing

Todos os entities implementam os campos de auditoria gerenciados automaticamente pelo Spring Data JPA:

```java
@CreatedDate    → created_at   (preenchido na inserção, imutável)
@LastModifiedDate → updated_at (atualizado em cada modificação)
@CreatedBy      → created_by  (email do usuário autenticado, imutável)
@LastModifiedBy → updated_by  (email do usuário autenticado)
```

O `AuditingEntityListener` é configurado em `AuditorAwareImpl.java`, que lê o principal do `SecurityContextHolder`.

### 3.6 Geração Automática de Slug

As entidades `ProductCategory`, `Supplier` e `Product` geram seu slug automaticamente via `@PrePersist` / `@PreUpdate`:

```java
@PrePersist
@PreUpdate
private void generateSlug() {
    this.slug = StringUtils.toSlug(this.name);
}
```

`StringUtils.toSlug()` converte o nome para lowercase, remove acentos e substitui espaços por hífens.

---

## 4. Infraestrutura de Segurança

### 4.1 JWT (Auth0)

- **Library:** `com.auth0:java-jwt:4.4.0`
- **Algoritmo:** HMAC256 com secret configurável via `api.security.token.secret`
- **Emissor:** `auth-api`
- **Subject:** email do usuário
- **Expiração:** 2 horas (fuso `-03:00`)
- **Suporte a refresh:** `validateAndGetSubjectEvenIfExpired()` aceita tokens expirados para renovação
- **Cookies:** Token armazenado em cookie `fitoherb_jwt` (HttpOnly) e email em `fitoherb_user_email`

### 4.2 CORS

Origens permitidas configuradas em `SecurityConfigurations.corsConfigurationSource()`:

```
http://localhost:4200                                          (desenvolvimento)
https://fitoherb.web.app                                      (Firebase)
https://fitoherb.com.br / https://www.fitoherb.com.br         (produção)
https://qa.fitoherb.com.br                                    (QA)
https://fitoherb-frontend-prod-*.us-central1.run.app          (Cloud Run)
https://fitoherb-frontend-qa-*.us-central1.run.app            (Cloud Run QA)
```

Métodos: `GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH`  
Headers: todos permitidos (`*`)  
Credentials: `true` (necessário para cookies)

### 4.3 Senhas

- Algoritmo: `BCryptPasswordEncoder` (custo padrão)
- No registro, uma senha aleatória de 10 caracteres é gerada via `SecureRandom` e enviada por e-mail ao usuário

---

## 5. Armazenamento de Arquivos (Strategy Pattern)

A interface `FileStorageService` define o contrato de armazenamento de imagens:

```java
public interface FileStorageService {
    String storeSupplierImage(MultipartFile file);
    void deleteSupplierImage(String fileName);

    String storeCategoryImage(MultipartFile file);
    void deleteCategoryImage(String fileName);

    String storeProductImage(MultipartFile file);
    void deleteProductImage(String fileName);

    String storeBannerImage(MultipartFile file);
    void deleteBannerImage(String fileName);
}
```

### Implementações

| Implementação | Quando usar | Configuração |
|--------------|-------------|-------------|
| `LocalFileStorageService` | Desenvolvimento local | `app.storage.local.path` no `application.properties` |
| `GcsFileStorageService` | Produção (Google Cloud) | Credenciais GCS via variável de ambiente |

A seleção da implementação ativa é feita via `@ConditionalOnProperty` ou `@Primary` no contexto Spring.

> **Nota:** A biblioteca `net.coobird:thumbnailator:0.4.20` é utilizada para redimensionamento/compressão de imagens antes do armazenamento.

---

## 6. Ambiente Docker

O projeto conta com um `Dockerfile` multi-stage:

```dockerfile
# Estágio 1: Build (eclipse-temurin:21-jdk-alpine)
#   - Copia gradlew, gradle, build.gradle, settings.gradle, src
#   - Executa: ./gradlew build -x test --no-daemon
#   - Saída: build/libs/fitoherb-backend-v2-0.0.1-SNAPSHOT.jar

# Estágio 2: Runtime (eclipse-temurin:21-jre-alpine)
#   - Copia apenas o .jar do estágio de build
#   - EXPOSE 8080
#   - ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Variáveis de Ambiente obrigatórias no container:**

| Variável | Descrição |
|----------|-----------|
| `PostgresUsername` | Usuário do banco de dados PostgreSQL |
| `PostgresPassword` | Senha do banco (usada também como secret JWT) |
| `mailSenderUsername` | E-mail remetente (SMTP Gmail) |
| `mailSenderPassword` | Senha de app do Gmail |
| `GOOGLE_APPLICATION_CREDENTIALS` | Caminho para o JSON de credenciais GCS (produção) |

---

## 7. Documentação Interativa (Swagger/OpenAPI 3)

- **Library:** `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5`
- **URL:** `http://localhost:8080/swagger-ui.html`
- **Spec JSON:** `http://localhost:8080/v3/api-docs`
- Todos os endpoints possuem `@Operation`, `@ApiResponses` e `@Tag` documentando contratos e exemplos de erro
- Suporte a autorização JWT diretamente pelo botão "Authorize" do Swagger UI

---

## 8. Estrutura de Pacotes

```
com.fitoherb.fitoherb_backend_v2/
├── controllers/          # @RestController — entrada HTTP
├── dtos/
│   ├── requests/         # DTOs de entrada (LoginReq, ProductReq, ...)
│   └── responses/        # DTOs de saída (UserRes, ProductRes, ...)
├── entities/             # @Entity JPA — mapeamento objeto-relacional
├── enums/                # UserRole (ADMIN, USER)
├── exceptions/           # Exceções de domínio tipadas
├── infra/
│   ├── config/           # Configurações gerais da aplicação
│   ├── exceptions/       # RestExceptionHandler, RestErrorMessage, ...
│   └── security/         # SecurityConfigurations, SecurityFilter, AuditorAwareImpl
├── mappers/              # Interfaces MapStruct
├── repositories/         # Interfaces Spring Data JPA
├── services/             # Lógica de negócio e serviços auxiliares
└── utils/                # StringUtils, ValidationConstants, ...
```
