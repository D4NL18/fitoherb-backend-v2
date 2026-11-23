# Entidade / Feature: Bypass Automático (Fail-Open) na Validação ERP (`P-403`) e Correção de HTTP 503 no `ErpSqlServerGateway`

Este documento centraliza o planejamento, as especificações e as etapas técnicas atômicas para a flexibilização resiliente da regra `P-403` em `POST /products` / `PUT /products/{slug}` e a correção da consulta SQL no `ErpSqlServerGateway` que causava erro `503 Service Unavailable` em produção.

## 1. Dúvidas Resolvidas (Especificação)
- **Dúvida 1 (`P-403` — Indisponibilidade do ERP durante cadastro/edição de produtos):** O que o backend deve fazer em `POST /products` e `PUT /products/{slug}` quando o SQL Server do ERP Atrio estiver inacessível (`ServiceUnavailableException`)?
  - **Decisão:** Adotar **bypass automático (*fail-open*)** apenas das validações remotas no ERP (`P-403` e `P-406`). Quando o ERP estiver disponível, mantém-se o modo estrito (`422 ERP_CODE_NOT_FOUND`, `422 ERP_SKU_INACTIVE_CONFIRMATION_REQUIRED`, `erpLastVerifiedAt = now`). Quando indisponível, mantêm-se intactas todas as validações locais (`P-402`, `P-404`, `P-405`), registra-se `log.warn` sanitizado (sem credenciais/PII) e persiste-se o `ProductSku` com `erpLastVerifiedAt = null`, `ean = null`, `erpInactive = false`, `erpInactiveConfirmed = false` para verificação posterior no sync diário (`P-410`). Endpoints exclusivos do ERP (`/erp-products/*` e `/erp-products/sync/*`) permanecem retornando HTTP `503` (`P-410`).
- **Dúvida 2 (Causa raiz do `503` em produção no `ErpSqlServerGateway`):** Quais ajustes SQL e de observabilidade são necessários no gateway somente-leitura?
  - **Decisão:** Corrigir os `LEFT JOIN` para usar as chaves compostas reais do ERP (`t.Id_TpPro = p.tipo_pro AND t.Empresa_TpPro = p.Empresa_Pro` e `m.ID_Marc = p.Marca_Pro AND m.Empresa_Marc = p.Empresa_Pro`), remover `LEFT JOIN [Externo01].[Produto]` (substituindo por `CAST(0 AS BIT) AS in_externo_01` para eliminar `SocketTimeoutException` > 15s) e aprimorar o logging sanitizado registrando `SQLState`, `errorCode`, classe da exceção/causa e ausência de configuração `host`/`username` sem expor credenciais ou PII.

---

## 2. Tarefas e Etapas de Desenvolvimento

### Tarefa 1: Bypass Automático (*Fail-Open*) na Validação ERP (`P-403`)
- [x] **Etapa 1.1:** Atualizar `ErpCatalogValidationService` para capturar `ServiceUnavailableException` na consulta ao `ErpCatalogCacheService`, registrar `log.warn` sanitizado indicando bypass por indisponibilidade do ERP, interromper o loop remoto e prosseguir para a checagem de conflito global `P-404` no PostgreSQL retornando mapa vazio de snapshots.
- [x] **Etapa 1.2:** Ajustar `ProductService.applySkusAndProjection` para que, quando não houver snapshot ERP para o `erpCode` (cenário de bypass), o `ProductSku` seja persistido com `erpLastVerifiedAt = null`, `ean = null`, `erpInactive = false` e `erpInactiveConfirmed = false`; e quando houver snapshot, continue gravando `erpLastVerifiedAt = now` e os metadados do snapshot.

### Tarefa 2: Correção da Query SQL e Observabilidade Sanitizada em `ErpSqlServerGateway`
- [x] **Etapa 2.1:** Atualizar `ALLOWLIST_CATALOG_QUERY` em `ErpSqlServerGateway` corrigindo os `LEFT JOIN` de `[dbo].[TipoProd]` (`t.Id_TpPro = p.tipo_pro AND t.Empresa_TpPro = p.Empresa_Pro`) e `[dbo].[Marca]` (`m.ID_Marc = p.Marca_Pro AND m.Empresa_Marc = p.Empresa_Pro`) e removendo `LEFT JOIN [Externo01].[Produto]` (retornando `CAST(0 AS BIT) AS in_externo_01`).
- [x] **Etapa 2.2:** Aprimorar o logging sanitizado em `ErpSqlServerGateway` para registrar ausência de `host`/`username` e, em `SQLException`, registrar `SQLState`, `errorCode` e classe da exceção/causa sem expor credenciais ou PII.

### Tarefa 3: Suíte de Testes TDD (`ErpCatalogValidationServiceTest`, `ProductServiceTest`, `ErpCatalogGatewayTest`)
- [x] **Etapa 3.1:** Atualizar/criar testes TDD em `ErpCatalogValidationServiceTest`.
- [x] **Etapa 3.2:** Atualizar/criar testes TDD em `ProductServiceTest`.
- [x] **Etapa 3.3:** Atualizar/criar testes TDD em `ErpCatalogGatewayTest`.
- [x] **Etapa 3.4:** Executar `./gradlew test` completo garantindo 0 falhas e 0 regressões.
