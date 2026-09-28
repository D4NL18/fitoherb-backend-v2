# Regras de Negócio: Rotas Agendadas com Calendário (Scheduled Routes)

## 1. Descrição Geral
O módulo de Rotas Agendadas permite que vendedores (SELLER) e administradores (ADMIN) salvem rotas otimizadas vinculadas a datas específicas através de um calendário interativo. Isso possibilita o planejamento antecipado das entregas ou visitas.

## 2. Regras de Negócio Core (P-XXX)

**P-200 - Persistência de Rotas Agendadas:**
O sistema DEVE permitir que usuários com perfil SELLER ou ADMIN salvem rotas otimizadas vinculadas a uma data específica (`route_date`). Cada rota armazena o ponto de partida (`depot` como JSON), lista de paradas (`stops` como JSON), resultado da otimização da IA (`optimization_result` como JSON), horário de partida (`departure_time`) e se retorna à base (`return_to_depot`). Um vendedor pode ter no máximo uma rota por data.

**P-201 - Janela de Agendamento de 1 Mês:**
O sistema DEVE permitir agendar rotas para qualquer data entre HOJE e HOJE + 30 dias (inclusive). Se hoje é 28/09/2026, as datas válidas vão de 28/09/2026 até 28/10/2026. Tentativas de agendar fora dessa janela devem retornar HTTP 422 com mensagem "Data fora da janela de agendamento permitida (hoje até 30 dias à frente).".

**P-202 - Janela de Visualização de Histórico de 7 Dias:**
O sistema DEVE permitir visualizar rotas salvas de até 7 dias no passado a partir de HOJE. Se hoje é 10/09, o usuário pode ver rotas do dia 03/09 em diante. Rotas anteriores ao limite de 7 dias não devem ser retornadas ao frontend.

**P-203 - Limpeza Automática de Rotas Expiradas:**
O sistema DEVE executar diariamente (via `@Scheduled` cron) um job de limpeza que deleta permanentemente do banco de dados todas as rotas com `route_date` anterior a (HOJE - 7 dias). O job deve logar a quantidade de registros removidos sem expor dados pessoais.

**P-204 - Unicidade de Rota por Data e Usuário:**
O sistema DEVE garantir que cada vendedor tenha no máximo uma rota salva por data. Se o vendedor salvar uma nova rota para uma data que já possui rota, a rota anterior DEVE ser substituída (upsert). Constraint UNIQUE em (`user_id`, `route_date`).

**P-205 - Isolamento de Dados por Usuário:**
Cada vendedor só pode visualizar, criar, atualizar e deletar suas próprias rotas agendadas. Administradores (ADMIN) podem visualizar rotas de qualquer vendedor. Tentativas de acesso não autorizado retornam HTTP 403.

**P-206 - Resumo de Datas com Rotas para o Calendário:**
O sistema DEVE fornecer um endpoint leve que retorne apenas as datas que possuem rotas salvas para o usuário autenticado, dentro da janela visível (hoje-7 dias até hoje+30 dias), para que o frontend possa exibir indicadores visuais no calendário sem carregar dados pesados.

## 3. Conformidade com a LGPD (Privacy by Design)
- **Base Legal:** Execução de contrato (Art. 7º, V). O processamento de latitude/longitude e endereços dos clientes é necessário para o cálculo viário e planejamento logístico das entregas.
- **Minimização de Dados:** As rotas expiram e são fisicamente deletadas após 7 dias de histórico.
- **Segurança:** O log do job de limpeza não deve expor IDs de usuários ou coordenadas.

## 4. Casos de Uso (Cenários e Edge Cases)

**Cenário 1: Salvando uma nova rota na janela permitida**
- **Dado** que o vendedor selecionou a data de amanhã
- **Quando** ele envia a rota otimizada
- **Então** o sistema salva os dados e retorna sucesso.

**Cenário 2: Substituindo rota existente**
- **Dado** que o vendedor já tem uma rota para 05/10/2026
- **Quando** ele envia uma nova rota para 05/10/2026
- **Então** o sistema atualiza o registro existente em vez de criar um novo.

**Edge Case 1: Tentativa de agendamento em data inválida (Passado)**
- **Dado** que hoje é 28/09/2026
- **Quando** o vendedor tenta salvar uma rota para 25/09/2026
- **Então** a API recusa com erro 422.

**Edge Case 2: Tentativa de agendamento em data inválida (Muito futuro)**
- **Dado** que hoje é 28/09/2026
- **Quando** o vendedor tenta salvar uma rota para 25/11/2026
- **Então** a API recusa com erro 422.
