# Regras de Negócio — Módulo de E-mail (Mail)

> **Módulo:** Notificações e Televendas  
> **Identificador:** RN-MAIL-001 a RN-MAIL-004

---

## RN-MAIL-001: Validação de Campos Obrigatórios
- O nome do remetente deve conter entre 3 e 150 caracteres.
- O e-mail deve ser sintaticamente válido (formato RFC 5322).
- O telefone deve ser preenchido para contato posterior pela equipe de televendas.
- A mensagem deve possuir no mínimo 10 caracteres e no máximo 2000 caracteres.

---

## RN-MAIL-002: Proteção contra Falhas de Conexão SMTP
- Caso o servidor SMTP (Gmail) falhe ou haja timeout na autenticação, o serviço captura a exceção de rede e dispara `MailSendingException`.
- A mensagem de erro retornada para o cliente não deve expor credenciais nem senhas de aplicativo (`mailSenderPassword`).

---

## RN-MAIL-003: Higienização de Conteúdo (Anti-Header Injection)
- Quebras de linha nos campos de assunto e nome são sanitizadas para evitar ataques de injeção de cabeçalhos SMTP (Email Header Injection).

---

## RN-MAIL-004: Destinatário Institucional
- Toda mensagem recebida no formulário é despachada para o e-mail oficial da Fitoherb Nordeste configurado na propriedade `mailSenderUsername`.
