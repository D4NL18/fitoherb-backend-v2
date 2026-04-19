# Fitoherb Backend v2

Este é o repositório do backend da aplicação **Fitoherb**, construído na sua versão 2. O projeto consiste em uma API RESTful robusta desenvolvida em **Java** com o framework **Spring Boot**. A API é responsável por gerenciar usuários, autenticação, produtos, categorias de produtos e fornecedores da plataforma Fitoherb.

## 🚀 Tecnologias Utilizadas

O projeto utiliza as seguintes tecnologias e bibliotecas:

* **Java 26**
* **Spring Boot 4.0.5**: Framework principal do projeto.
* **Spring Data JPA & Hibernate**: Para persistência e mapeamento objeto-relacional.
* **PostgreSQL**: Banco de dados relacional.
* **Spring Security**: Para controle de acesso e segurança da aplicação.
* **JWT (Auth0)**: Para autenticação *stateless* baseada em tokens.
* **Springdoc OpenAPI (Swagger)**: Para documentação interativa da API.
* **Lombok**: Para reduzir a verbosidade do código (getters, setters, construtores).
* **MapStruct**: Para o mapeamento de objetos (Entities para DTOs e vice-versa).
* **Spring Boot Mail**: Para o envio de e-mails.
* **Gradle**: Ferramenta de build e gestão de dependências.

---

## 🔒 Segurança

A segurança foi uma prioridade no desenvolvimento desta API. As seguintes ações foram tomadas:

* **Autenticação Stateless com JWT**: A API não guarda sessão no servidor (`SessionCreationPolicy.STATELESS`). Toda requisição autenticada deve incluir um token JWT no cabeçalho `Authorization: Bearer <token>`.
* **Criptografia de Senhas**: As senhas dos usuários são *hasheadas* utilizando o algoritmo **BCrypt** (`BCryptPasswordEncoder`) antes de serem persistidas no banco de dados.
* **Filtros de Segurança Customizados**: Um filtro customizado (`SecurityFilter`) intercepta as requisições para validar a presença, integridade e expiração do token JWT.
* **Prevenção contra CSRF**: Como a API é *stateless* e consumida via tokens no cabeçalho, a proteção CSRF (Cross-Site Request Forgery) foi desabilitada, o que é o padrão e recomendado para APIs RESTful JWT.

---

## 📖 Documentação da API com Swagger

O projeto utiliza o **Swagger** (via Springdoc OpenAPI) para gerar uma documentação interativa e visual dos endpoints disponíveis.
Com o servidor rodando, você pode acessar a interface do Swagger UI através da URL:

```
http://localhost:8080/swagger-ui.html
```
*(A porta padrão pode variar conforme a configuração local)*

No Swagger UI, é possível visualizar todos os endpoints, os esquemas de dados esperados (DTOs) e testar requisições diretamente pelo navegador. A autorização via JWT também é suportada diretamente na interface (botão "Authorize").

---

## 🛡️ Qualidade de Código e Segurança com SonarQube

O **SonarQube** é sugerido como ferramenta de análise estática de código (Code Quality & Security) para este projeto. O seu uso contínuo garante:

* **Detecção de Bugs e Code Smells**: Identificação de trechos de código que podem causar erros em tempo de execução ou que ferem as boas práticas de desenvolvimento Java.
* **Análise de Vulnerabilidades**: O SonarQube detecta potenciais falhas de segurança no código (ex: Injections, exposição de dados sensíveis).
* **Cobertura de Testes**: Ajuda a monitorar o percentual do código que está sendo coberto pelos testes unitários (JUnit).

**Como utilizar o SonarQube no projeto:**
Você pode rodar um container Docker do SonarQube localmente (`docker run -d --name sonarqube -p 9000:9000 sonarqube`) e integrar com o build do Gradle executando a task do Sonar Scanner (após configurar o plugin no `build.gradle`), ou configurá-lo na pipeline de CI/CD (ex: GitHub Actions) para análise automatizada a cada *Push* ou *Pull Request*.

---

## 🛣️ Endpoints Principais

Abaixo estão as rotas principais disponíveis na API:

### Autenticação (`/auth`)
* `POST /auth/login`: Realiza o login na plataforma e retorna o token JWT.
* `POST /auth/register`: Registra um novo usuário no sistema.

### Usuários (`/users`)
* `GET /users`: Lista os usuários.
* `GET /users/{email}`: Busca um usuário específico por e-mail.
* `PUT /users/{email}`: Atualiza os dados do usuário.
* `DELETE /users/{email}`: Remove um usuário.

### Produtos (`/products`)
* `GET /products`: Lista os produtos.
* `GET /products/{slug}`: Busca um produto específico pelo seu *slug*.
* `POST /products`: Cria um novo produto (suporta envio de imagens/arquivos via multipart).
* `PUT /products/{slug}`: Atualiza um produto existente.
* `DELETE /products/{slug}`: Remove um produto.
* `GET /products/gallery`: Retorna a galeria de produtos.

### Categorias de Produtos (`/product_categories`)
* `GET /product_categories`, `GET /product_categories/get-all`: Listagem de categorias.
* `GET /product_categories/{slug}`: Busca uma categoria por *slug*.
* `POST /product_categories`, `PUT /product_categories/{slug}`, `DELETE /product_categories/{slug}`: Gerenciamento (criação, edição e exclusão).

### Fornecedores (`/suppliers`)
* `GET /suppliers`, `GET /suppliers/get-all`: Listagem de fornecedores.
* `GET /suppliers/{slug}`: Busca um fornecedor por *slug*.
* `POST /suppliers`, `PUT /suppliers/{slug}`, `DELETE /suppliers/{slug}`: Gerenciamento (criação, edição e exclusão).

---

## ⚙️ Configuração e Execução do Projeto

### Pré-requisitos
* **Java Development Kit (JDK) 26** instalado.
* **PostgreSQL** rodando localmente (na porta padrão 5432).
* **Variáveis de Ambiente**: A aplicação requer algumas variáveis de ambiente para rodar. Configure-as no seu SO ou no perfil de execução da sua IDE:
  * `PostgresUsername`: Usuário do banco de dados.
  * `PostgresPassword`: Senha do banco (usada também como *secret* para gerar o JWT na configuração atual).
  * `mailSenderUsername`: E-mail remetente (SMTP do Gmail).
  * `mailSenderPassword`: Senha de aplicativo do Gmail.
* Crie os diretórios de imagens configurados no `application.properties`:
  * `C:\Users\PC\Documents\FitoherbImages/Suppliers`
  * `C:\Users\PC\Documents\FitoherbImages/Products`
  * `C:\Users\PC\Documents\FitoherbImages/Categories`
*(Sinta-se à vontade para alterar os caminhos no `application.properties` se estiver em um ambiente Linux/Mac ou em pastas diferentes).*

### 💻 Como rodar no Visual Studio Code (VS Code)

1. **Extensões necessárias:** Instale o **Extension Pack for Java** e o **Spring Boot Extension Pack**.
2. Abra a pasta do projeto `fitoherb-backend-v2` no VS Code.
3. O VS Code detectará que é um projeto Java/Gradle. Aguarde o download das dependências (você pode verificar o progresso na aba "Java Projects" ou rodando `gradle build`).
4. Configure as variáveis de ambiente necessárias no arquivo `launch.json` ou exporte-as no terminal integrado.
5. Abra a classe principal (ex: `FitoherbBackendV2Application.java`) e clique em **Run** ou **Debug** no *Code Lens* logo acima do método `main`.
6. Alternativamente, acesse a aba "Spring Boot Dashboard" e inicie a aplicação por lá.

### 💻 Como rodar no IntelliJ IDEA

1. Abra o IntelliJ IDEA e selecione **Open**.
2. Navegue até a pasta `fitoherb-backend-v2` e clique em OK.
3. O IntelliJ reconhecerá automaticamente o arquivo `build.gradle`. Se perguntado, clique em **Load Gradle Project**.
4. Vá em `File > Project Structure` e certifique-se de que o **SDK** e o **Language Level** estão configurados para o **Java 26**.
5. Crie ou edite a configuração de execução (*Run/Debug Configurations*):
   * Clique em `Edit Configurations...` (próximo ao botão de *Run/Play* no topo direito).
   * Selecione a classe main da aplicação Spring Boot.
   * Na aba **Environment variables**, adicione as variáveis requeridas (`PostgresUsername`, `PostgresPassword`, etc).
6. Clique no botão de **Run** (Shift + F10) ou **Debug** (Shift + F9).

---

> **Desenvolvido para Fitoherb**
