# Contratos de API — Módulo de E-mail (Mail)

> **Prefixo de Rota:** `/email`  
> **Autenticação:** Pública

---

## 1. `POST /email/send` — Envio de Mensagem de Contato

Dispara e-mail formatado para a equipe comercial com os dados fornecidos pelo usuário no formulário do site institucional.

### Requisição
- **Headers:** `Content-Type: application/json`
- **Body:** `MailReq`

```json
{
  "name": "Maria Oliveira",
  "email": "maria.oliveira@farmacia.com.br",
  "phone": "(71) 98888-7777",
  "message": "Gostaria de solicitar uma tabela de preços no atacado para os chás orgânicos e cápsulas fitoterápicas para revenda em Salvador."
}
```

### Respostas

#### `200 OK`
```json
{
  "message": "Email sent successfully"
}
```

#### `400 Bad Request` — Validação de Campos
```json
{
  "status": "BAD_REQUEST",
  "message": "Validation failed for one or more fields",
  "errors": {
    "email": "must be a well-formed email address",
    "name": "must not be blank"
  }
}
```

#### `500 Internal Server Error` — Erro no Servidor SMTP
```json
{
  "status": "INTERNAL_SERVER_ERROR",
  "message": "Failed to send email due to mail server error."
}
```
