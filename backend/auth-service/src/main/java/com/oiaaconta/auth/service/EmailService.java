package com.oiaaconta.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final ContratoPdfService contratoPdfService;

    @Value("${email.from:martiriotecnologia@gmail.com}")
    private String from = "martiriotecnologia@gmail.com";

    @Value("${email.from-name:Oia a Conta}")
    private String fromName = "Oia a Conta";

    @Value("${spring.mail.password:}")
    private String mailPassword = "";

    @Value("${frontend.url:https://oiaaconta.com.br}")
    private String frontendUrl = "https://oiaaconta.com.br";

    @Value("${suporte.contato:noreply.oiaaconta@gmail.com}")
    private String contatoSuporte = "noreply.oiaaconta@gmail.com";

    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

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

    // Além das boas-vindas, anexa o PDF do Contrato de Adesão aceito na
    // contratação — planoNome/planoPreco/versaoContrato/aceitoEm/contratoId
    // vêm do Contrato criado no billing-service (ver
    // AuthService.criarContaFromPendente), não de RegistroPendente, pra
    // garantir que o valor/plano no e-mail batem com o que foi efetivamente
    // contratado. Quando o billing-service não responde (ex: fora do ar), os
    // dados do plano ficam null e o e-mail sai sem o anexo — perda
    // aceitável frente a bloquear todo o cadastro por causa disso.
    @Async
    public void enviarBoasVindas(String destinatario, String nomeDestinatario, String nomeRestaurante,
                                  String planoNome, BigDecimal planoPreco, String versaoContrato,
                                  LocalDateTime aceitoEm, Long contratoId) {
        String precoFormatado = planoPreco == null ? "—" :
            planoPreco.toPlainString().replace(".", ",");
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px">
              <h2 style="color:#2563eb">Bem-vindo ao Oia a Conta! 🎉</h2>
              <p>Olá, <strong>%s</strong>!</p>
              <p>A empresa <strong>%s</strong> foi cadastrada com sucesso e sua contratação foi concluída.</p>

              <h3 style="margin-top:24px">Dados da contratação</h3>
              <p style="margin:4px 0"><strong>Plano contratado:</strong> %s</p>
              <p style="margin:4px 0"><strong>Valor:</strong> R$ %s/mês</p>
              <p style="margin:4px 0"><strong>Forma de pagamento:</strong> PIX</p>

              <p>Seu acesso à plataforma já está disponível.</p>
              <div style="text-align:center;margin:24px 0">
                <a href="%s" style="background:#2563eb;color:#fff;padding:12px 28px;border-radius:8px;text-decoration:none;font-weight:bold">Acessar painel</a>
              </div>

              <h3 style="margin-top:24px">Contrato de Adesão</h3>
              <p>
                Conforme o aceite realizado durante a contratação através do portal Oia a Conta, encaminhamos
                anexado a este e-mail o PDF com uma cópia do Contrato de Adesão correspondente ao plano
                contratado. O documento anexado corresponde à versão apresentada e aceita no momento da
                contratação.
              </p>
              <p style="margin:4px 0"><strong>Versão do contrato:</strong> %s</p>
              <p style="margin:4px 0"><strong>Data e hora do aceite:</strong> %s</p>
              <p style="margin:4px 0"><strong>Identificação da contratação:</strong> #%s</p>

              <p>Recomendamos que você mantenha este e-mail e o contrato anexado para seus registros.</p>
              <p>Em caso de dúvidas ou necessidade de suporte, nossa equipe está à disposição pelos canais oficiais de atendimento.</p>
              <p>Obrigado por escolher o Oia a Conta.</p>

              <p style="margin-top:24px">Atenciosamente,<br><strong>Equipe Oia a Conta</strong></p>
              <p style="color:#6b7280;font-size:12px">%s · %s</p>
            </div>
            """.formatted(
                nomeDestinatario, nomeRestaurante,
                planoNome == null ? "—" : planoNome, precoFormatado,
                frontendUrl,
                versaoContrato == null ? "—" : versaoContrato,
                aceitoEm == null ? "—" : aceitoEm.format(FORMATO_DATA_HORA),
                contratoId == null ? "—" : contratoId,
                frontendUrl, contatoSuporte
            );

        byte[] pdf = null;
        if (planoNome != null && planoPreco != null && versaoContrato != null && aceitoEm != null && contratoId != null) {
            try {
                pdf = contratoPdfService.gerarContratoAdesao(planoNome, planoPreco, versaoContrato, aceitoEm, contratoId);
            } catch (Exception e) {
                log.warn("Falha ao gerar PDF do contrato para {}: {}", destinatario, e.getMessage());
            }
        }

        enviar(destinatario, "Bem-vindo ao Oia a Conta!", html, pdf, "Contrato de Adesao - Oia a Conta.pdf");
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

    private void enviar(String destinatario, String assunto, String htmlContent) {
        enviar(destinatario, assunto, htmlContent, null, null);
    }

    @SuppressWarnings("null")
    private void enviar(String destinatario, String assunto, String htmlContent, byte[] anexoPdf, String nomeAnexo) {
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
            if (anexoPdf != null && anexoPdf.length > 0) {
                helper.addAttachment(nomeAnexo, new ByteArrayResource(anexoPdf));
            }
            mailSender.send(message);
            log.info("E-mail enviado para {}", destino);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail para {}: {}", destino, e.getMessage());
        }
    }
}
