# Current Workflow State: CORS Fix para QA

1. **Especificar (Specification)**: QA não está conseguindo acessar o back via frontend de QA por conta de erro de CORS (origin `https://qa.fitoherb.com.br` blocked).
2. **Projetar (Design & Architecture)**: Adicionar `https://qa.fitoherb.com.br` às configurações de CORS em `SecurityConfigurations.java` e `WebConfig.java`.
3. **Modelagem de Dados Segura (DBA)**: N/A.
4. **Planejar as Tarefas (Task Planning)**: Ver checklist em `docs/tasks/cors_qa_fix.md`.
5. **Desenvolver Testes Unitários (TDD)**: Como é uma mudança de configuração de ambiente e security config, os testes de integração ou MVC de CORS podem ser verificados (N/A testes unitários para a lista específica).
6. **Executar (Execution)**: Adicionar a URL no código.
7. **Code Review (Manutenibilidade)**: Código validado quanto a Clean Code.
8. **Testar (Validation & QA)**: Validação manual / auditoria de build.
9. **Auditoria de Segurança (SecOps)**: URLs em hardcoded whitelist (adequado). Sem origin '*'.
10. **Release via Pull Request (DevOps)**: Gerar PR para `develop`.
