# Objetivos do Sistema — Módulo de E-mail (Mail)

> **Módulo:** Notificações e Formulário de Contato  
> **Sistema:** Fitoherb Backend v2

---

## 1. Objetivo Principal
Prover um canal direto, seguro e assíncrono para captação de leads, novos lojistas, farmácias de manipulação e consumidores que desejam estabelecer parceria comercial ou tirar dúvidas sobre o catálogo de produtos naturais da Fitoherb.

---

## 2. Objetivos Técnicos e Operacionais
1. **Desacoplamento:** Isolar a infraestrutura de envio de mensagens do restante do domínio de produtos e pedidos.
2. **Resiliência:** Garantir tratamento de falha gracioso sem derrubar o container em caso de indisponibilidade momentânea do provedor SMTP.
3. **Privacidade e LGPD:** Tratar os dados de contato do remetente exclusivamente para finalidade de resposta ao lead comercial.
