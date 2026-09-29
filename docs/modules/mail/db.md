# Banco de Dados — Módulo de E-mail (Mail)

> **Módulo:** Notificações Transacionais  
> **Status:** Stateless / Sem Persistência Própria

---

## 1. Modelo de Dados

O módulo de e-mail opera em regime puramente transacional (`stateless`), não mantendo tabela relacional dedicada para armazenamento permanente das mensagens recebidas no PostgreSQL no estado atual da v2.

---

## 2. Auditoria e Rastreabilidade

- **Logs de Aplicação:** Os eventos de envio bem-sucedido ou falha são registrados no log estruturado da aplicação com nível `INFO` ou `ERROR`.
- **Campos Tratados:**
  - `name`: Nome do remetente
  - `email`: E-mail de retorno
  - `phone`: Telefone comercial
  - `message`: Corpo textual da mensagem

---

## 3. Planejamento Futuro (v2.1)

Para futura expansão, planeja-se a tabela `contact_messages`:
```sql
CREATE TABLE IF NOT EXISTS contact_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_name VARCHAR(150) NOT NULL,
    sender_email VARCHAR(150) NOT NULL,
    sender_phone VARCHAR(50),
    message TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```
