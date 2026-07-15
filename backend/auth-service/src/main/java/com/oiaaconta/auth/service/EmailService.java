package com.oiaaconta.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.from:martiriotecnologia@gmail.com}")
    private String from = "martiriotecnologia@gmail.com";

    @Value("${email.from-name:Oia a Conta}")
    private String fromName = "Oia a Conta";

    @Value("${spring.mail.password:}")
    private String mailPassword = "";

    @Async
    public void enviarCodigoVerificacao(String destinatario, String nomeDestinatario, String codigo) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:500px;margin:0 auto;padding:24px">
              <h2 style="color:#2563eb">Oia a Conta — Verificação de E-mail</h2>
              <p>Olá, <strong>%s</strong>!</p>
              <p>Use o código abaixo para confirmar seu e-mail. Ele expira em <strong>15 minutos</strong>.</p>
              <div style="background:#f0f4ff;border-radius:8px;padding:20px;text-align:center;margin:24px 0">
                <span style="font-size:36px;font-weight:bold;letter-spacing:8px;color:#2563eb">%s</span>
              </div>
              <p style="color:#6b7280;font-size:12px">Se você não solicitou este código, ignore este e-mail.</p>
            </div>
            """.formatted(nomeDestinatario, codigo);

        enviar(destinatario, "Seu código de verificação — Oia a Conta", html);
    }

    @Async
    public void logCodigoDesenvolvimento(String destinatario, String codigo) {
        if (mailPassword == null || mailPassword.isBlank()) {
            log.warn("\n" +
                "╔══════════════════════════════════════════════╗\n" +
                "║  [DEV] CÓDIGO DE VERIFICAÇÃO                ║\n" +
                "║  Destinatário : {}       \n" +
                "║  Código       : {}                           ║\n" +
                "╚══════════════════════════════════════════════╝",
                destinatario, codigo);
        }
    }

    @Async
    public void enviarBoasVindas(String destinatario, String nomeDestinatario, String nomeRestaurante) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:500px;margin:0 auto;padding:24px">
              <h2 style="color:#2563eb">Bem-vindo ao Oia a Conta! 🎉</h2>
              <p>Olá, <strong>%s</strong>!</p>
              <p>Sua empresa <strong>%s</strong> foi cadastrada com sucesso.</p>
              <p>Acesse o painel para começar a gerenciar seu restaurante.</p>
            </div>
            """.formatted(nomeDestinatario, nomeRestaurante);

        enviar(destinatario, "Bem-vindo ao Oia a Conta!", html);
    }

    @Async
    public void enviarSenhaTemporaria(String destinatario, String nomeDestinatario, String novaSenha) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:500px;margin:0 auto;padding:24px">
              <h2 style="color:#2563eb">Oia a Conta — Redefinição de Senha</h2>
              <p>Olá, <strong>%s</strong>!</p>
              <p>Sua senha foi redefinida pelo administrador da plataforma.</p>
              <div style="background:#f0f4ff;border-radius:8px;padding:20px;text-align:center;margin:24px 0">
                <p style="margin:0;color:#6b7280;font-size:13px">Nova senha temporária</p>
                <span style="font-size:24px;font-weight:bold;letter-spacing:4px;color:#2563eb">%s</span>
              </div>
              <p>Acesse o sistema e altere sua senha imediatamente.</p>
            </div>
            """.formatted(nomeDestinatario, novaSenha);

        enviar(destinatario, "Sua senha foi redefinida — Oia a Conta", html);
    }

    @Async
    public void enviarBloqueioTenant(String destinatario, String nomeRestaurante) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:500px;margin:0 auto;padding:24px">
              <h2 style="color:#dc2626">Serviço suspenso — Oia a Conta</h2>
              <p>Olá!</p>
              <p>O acesso da empresa <strong>%s</strong> foi suspenso por inadimplência.</p>
              <p>Para reativar, regularize seu pagamento ou entre em contato pelo suporte.</p>
            </div>
            """.formatted(nomeRestaurante);

        enviar(destinatario, "Serviço suspenso — regularize seu pagamento", html);
    }

    @SuppressWarnings("null")
    private void enviar(String destinatario, String assunto, String htmlContent) {
        String destino = destinatario == null ? "" : destinatario.trim();
        String assuntoFinal = assunto == null ? "" : assunto.trim();
        String conteudoHtml = htmlContent == null ? "" : htmlContent;

        if (mailPassword == null || mailPassword.isBlank()) {
            log.warn("GMAIL_APP_PASSWORD não configurada — e-mail para {} não enviado (assunto: {})", destino, assuntoFinal);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(destino);
            helper.setSubject(assuntoFinal);
            helper.setText(conteudoHtml, true);
            mailSender.send(message);
            log.info("E-mail enviado para {}", destino);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail para {}: {}", destino, e.getMessage());
        }
    }
}
