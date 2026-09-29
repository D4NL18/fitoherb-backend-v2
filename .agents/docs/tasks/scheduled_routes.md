# Checklist de Tarefas: Rotas Agendadas com Calendário

## Backend (fitoherb-backend-v2)

- [ ] 1. **Modelagem de Dados e Flyway:**
  - Criar o script V_XX__create_scheduled_routes.sql contendo a tabela `scheduled_routes` e os índices conforme documentação.
  - Implementar a Entidade JPA `ScheduledRoute` com os mapeamentos para JSONB nos campos `depot`, `stops` e `optimization_result`.

- [ ] 2. **DTOs e Mapeamento:**
  - Criar os DTOs de Request (`ScheduledRouteRequestDto`) e Response (`ScheduledRouteResponseDto`, `ScheduledRouteSummaryDto`).
  - Atualizar/Criar a interface do MapStruct (`ScheduledRouteMapper`) para conversão de e para DTOs.

- [ ] 3. **Camada de Repositório e Consultas:**
  - Criar `ScheduledRouteRepository` com métodos para buscar por `user_id` e janela de datas.
  - Criar query customizada otimizada para buscar apenas as datas (endpoint de calendário).

- [ ] 4. **Camada de Serviço (Regras de Negócio):**
  - Criar `ScheduledRouteService`.
  - Implementar lógica de Upsert com validação da janela de agendamento (P-201).
  - Validar e injetar dados do usuário autenticado no salvamento e nas buscas (P-205).
  - Implementar validação de histórico visível em consultas (P-202).

- [ ] 5. **Camada de Controladores:**
  - Criar `ScheduledRouteController` mapeando os endpoints REST da especificação de contratos.
  - Adicionar as anotações `@PreAuthorize` corretas e validações via `@Valid`.

- [ ] 6. **Job de Limpeza (Cleanup):**
  - Criar a classe `ScheduledRouteCleanupJob` (ou similar) marcada com `@Component`.
  - Adicionar método com `@Scheduled(cron = "0 0 2 * * ?")` (diário às 02h00) para exclusão de rotas passadas além de 7 dias (P-203).

- [ ] 7. **Testes e Qualidade:**
  - Implementar testes unitários e de integração no backend validando os cenários das regras P-200 a P-206.
  - Garantir cobertura acima do limite exigido no Sonar.

## Frontend (fitoherb-frontend-v2)

- [ ] 8. **Integração de Serviços API:**
  - Criar tipos TypeScript equivalentes aos DTOs (Request, Resumo, Detalhado).
  - Criar `ScheduledRouteService` no Angular para interagir com a nova API usando HttpClient.

- [ ] 9. **Componente de Calendário e UI:**
  - Desenvolver o componente modal do Calendário (UI responsiva com layout em grid de 7 colunas).
  - Implementar lógicas de renderização dos dias habilitados/desabilitados e com indicativos de rotas agendadas (pontos verdes).
  - Integrar botão de ativação do Calendário na barra superior da rota de vendedor (`SellerRoutesComponent`).

- [ ] 10. **Integração de Estado e Fluxo:**
  - Tratar cliques nas datas do calendário para carregar e hidratar automaticamente a UI com a rota correspondente.
  - Implementar carregamento e visualização correta do "Empty State" (P-202, datas sem paradas).

- [ ] 11. **Validação de Experiência do Usuário (Anti-IA Slop e Vibe Check):**
  - Validar a usabilidade nos dispositivos móveis e em resoluções de desktop.
  - Aplicar revisão seguindo os tokens e guides do Design System Fitoherb (cores, tipografia).
