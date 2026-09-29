# Módulo de E-mail e Notificações (Mail) — Fitoherb Backend v2

> **Data de criação:** 23/09/2026  
> **Serviço Responsável:** `MailService.java`  
> **Tecnologia:** Spring Boot Starter Mail (JavaMailSender) + SMTP Gmail  
> **Padrão:** Serviço Transacional de Envio de Mensagens

---

## 1. Visão Geral

O módulo **Mail** é responsável pelo recebimento, validação e encaminhamento de mensagens enviadas por clientes e parceiros através do formulário de contato do site institucional da Fitoherb Nordeste. Ele formata a mensagem com dados do remetente e a encaminha via SMTP para a caixa de entrada da equipe de televendas e atendimento da empresa.

---

## 2. Componentes do Módulo

- **Controller:** `EmailController.java` (`POST /email/send`)
- **Service:** `MailService.java`
- **DTO Request:** `dtos/requests/MailReq.java` (`name`, `email`, `phone`, `message`)
- **Exceções Customizadas:** `MailSendingException.java`
- **Configurações:** `application.properties` (`spring.mail.host`, `spring.mail.username`, `spring.mail.password`)
