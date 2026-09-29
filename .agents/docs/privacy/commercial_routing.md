# Revisão de Privacy by Design & Auditoria LGPD: Módulo de Rotas Comerciais

## 1. Enquadramento Legal (Art. 7º da LGPD - Lei nº 13.709/2018)
- **Base Legal:** Execução de Contrato e Procedimentos Preliminares (Art. 7º, V) e Legítimo Interesse do Controlador (Art. 7º, IX) para otimização logística das rotas de entrega e visita comercial da Fitoherb.
- **Minimização de Dados (Art. 6º, III):**
  - O sistema armazena apenas dados estritamente indispensáveis para o deslocamento veicular (endereço de entrega, nome do estabelecimento/contato e coordenadas calculadas).
  - **Não Exposição de Coordenadas:** As coordenadas brutas de latitude e longitude não são exportadas no PDF do vendedor nem expostas para clientes finais, permanecendo restritas aos cálculos internos viários no motor genético.
  - **Isolamento de Locais Salvos (Multi-Tenancy por Usuário):** Cada vendedor tem acesso estritamente aos seus próprios locais salvos e bases cadastradas (`user_id`), com validação no backend Spring Boot em nível de entidade e camada de serviço (`SavedLocationService`).

---

## 2. Auditoria de Logs e Segurança
- Logs do microserviço `fitoherb-ai` e backend `fitoherb-backend-v2` não registram senhas, números de cartão, nem dados sensíveis.
- O token JWT é transmitido de forma segura via cabeçalho `Authorization: Bearer` e validado em tempo de execução via `@authorizationService.isAdminOrSeller()`.
