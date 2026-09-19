package com.oiaaconta.auth.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Gera o PDF do Contrato de Adesão anexado no e-mail de boas-vindas (ver
// EmailService.enviarBoasVindas). Texto mantido em paralelo ao componente
// React ContratoAdesao.tsx (frontend/src/pages/public/ContratoAdesao.tsx) —
// os dois precisam ser atualizados juntos quando o contrato mudar, já que
// não há uma fonte de conteúdo compartilhada entre o backend Java e o
// frontend TypeScript.
@Service
public class ContratoPdfService {

    private static final String ENC = BaseFont.CP1252;
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private record Clausula(String titulo, String corpo) {}

    public byte[] gerarContratoAdesao(String nomeContratante, String nomeRestaurante, String email,
                                       String cnpj, String telefone,
                                       String planoNome, BigDecimal planoPreco, String versaoContrato,
                                       LocalDateTime aceitoEm, Long contratoId) {
        Document document = new Document(PageSize.A4, 56, 56, 56, 56);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, ENC, 15);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, ENC, 9, Font.ITALIC, Color.GRAY);
            Font secaoFont = FontFactory.getFont(FontFactory.HELVETICA, ENC, 11, Font.BOLD, new Color(0xCF, 0x46, 0x22));
            Font corpoFont = FontFactory.getFont(FontFactory.HELVETICA, ENC, 9.5f);
            Font rotuloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, ENC, 9.5f);

            Paragraph titulo = new Paragraph("Contrato de Adesão aos Serviços da Plataforma Oia a Conta", tituloFont);
            titulo.setSpacingAfter(4);
            document.add(titulo);

            Paragraph meta = new Paragraph(
                "Versão " + versaoContrato + " · Aceito em " + aceitoEm.format(FORMATO_DATA_HORA) +
                " · Contratação #" + contratoId, metaFont);
            meta.setSpacingAfter(16);
            document.add(meta);

            // Identificação do CONTRATANTE — sem isso o PDF não comprova de
            // quem é a contratação, só qual plano/versão foi aceito.
            Paragraph dadosTitulo = new Paragraph("Dados do Contratante", secaoFont);
            dadosTitulo.setSpacingAfter(4);
            document.add(dadosTitulo);

            document.add(linhaDado("Responsável:", nomeContratante, rotuloFont, corpoFont));
            document.add(linhaDado("Empresa/Restaurante:", nomeRestaurante, rotuloFont, corpoFont));
            document.add(linhaDado("E-mail:", email, rotuloFont, corpoFont));
            if (cnpj != null && !cnpj.isBlank()) {
                document.add(linhaDado("CNPJ:", cnpj, rotuloFont, corpoFont));
            }
            if (telefone != null && !telefone.isBlank()) {
                document.add(linhaDado("Telefone:", telefone, rotuloFont, corpoFont));
            }

            Paragraph espaco = new Paragraph(" ", corpoFont);
            espaco.setSpacingAfter(6);
            document.add(espaco);

            String precoFormatado = "R$ " + String.format("%,.2f", planoPreco).replace(",", "X").replace(".", ",").replace("X", ".");

            for (Clausula c : clausulas(planoNome, precoFormatado)) {
                Paragraph secao = new Paragraph(c.titulo(), secaoFont);
                secao.setSpacingBefore(10);
                secao.setSpacingAfter(4);
                document.add(secao);
                Paragraph corpo = new Paragraph(c.corpo(), corpoFont);
                corpo.setAlignment(Element.ALIGN_JUSTIFIED);
                document.add(corpo);
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            document.close();
            throw new RuntimeException("Falha ao gerar PDF do contrato", e);
        }
    }

    private Paragraph linhaDado(String rotulo, String valor, Font rotuloFont, Font valorFont) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(rotulo + " ", rotuloFont));
        p.add(new Chunk(valor == null || valor.isBlank() ? "—" : valor, valorFont));
        p.setSpacingAfter(2);
        return p;
    }

    private List<Clausula> clausulas(String planoNome, String precoFormatado) {
        return List.of(
            new Clausula("Preâmbulo",
                "Pelo presente instrumento, de um lado, a empresa responsável pela plataforma Oia a Conta " +
                "(\"CONTRATADA\"), e, de outro, a pessoa física ou jurídica que realizar o cadastro e contratar " +
                "qualquer plano disponibilizado na plataforma (\"CONTRATANTE\"), têm entre si estabelecidas as " +
                "condições de contratação a seguir. Ao criar uma conta, selecionar um plano e concluir a " +
                "contratação, o CONTRATANTE declara que leu, compreendeu e concorda com este contrato e com os " +
                "Termos de Uso e a Política de Privacidade da plataforma."),
            new Clausula("1. Objeto",
                "1.1. Prestação de serviços de software como serviço (SaaS) por meio da plataforma Oia a Conta, " +
                "destinada à gestão de estabelecimentos de alimentação e delivery, incluindo, conforme o plano " +
                "contratado: gestão de mesas, comandas e pedidos, cozinha, caixa, produtos, clientes, delivery, " +
                "relatórios, integrações com terceiros e demais funcionalidades disponibilizadas.\n" +
                "1.2. As funcionalidades poderão variar conforme o plano e ser atualizadas pela CONTRATADA, sem " +
                "prejuízo das condições comerciais contratadas."),
            new Clausula("2. Contratação e Aceite",
                "2.1. A contratação se completa com o cadastro, a seleção do plano e a manifestação de " +
                "concordância com este contrato.\n" +
                "2.2. O aceite eletrônico tem validade para comprovação da contratação.\n" +
                "2.3. O CONTRATANTE responde pela veracidade e atualização das informações do cadastro."),
            new Clausula("3. Plano e Valor",
                "3.1. Plano contratado: " + planoNome + ", pelo valor de " + precoFormatado + " por mês.\n" +
                "3.2. Este contrato aplica-se também aos demais planos da plataforma, prevalecendo em cada caso " +
                "o plano, preço e condições apresentados no momento da contratação ou alteração.\n" +
                "3.3. Valores poderão ser alterados para futuras contratações/renovações, mediante comunicação " +
                "prévia, respeitada a legislação aplicável."),
            new Clausula("4. Forma e Condições de Pagamento",
                "4.1. Cobrança mensal via PIX, conforme condições apresentadas na contratação.\n" +
                "4.2. Vencimento mensal na data correspondente à ativação do plano.\n" +
                "4.3. A ausência de pagamento até o vencimento caracteriza inadimplência.\n" +
                "4.4. Em inadimplência, o acesso poderá ser restrito após 3 (três) dias úteis do vencimento.\n" +
                "4.5. Regularizado o pagamento, o acesso é restabelecido conforme os procedimentos técnicos."),
            new Clausula("5. Cancelamento",
                "5.1. Sem prazo mínimo de permanência ou fidelidade.\n" +
                "5.2. Cancelamento a qualquer momento, sem multa ou taxa.\n" +
                "5.3. Solicitável pelo painel da plataforma ou pelos canais oficiais de atendimento.\n" +
                "5.4. Cancelado após o pagamento do período em curso, o acesso segue disponível até o fim desse " +
                "período, sem novas cobranças.\n" +
                "5.5. Sem prejuízo do direito de arrependimento previsto na cláusula 6."),
            new Clausula("6. Direito de Arrependimento",
                "6.1. Nos termos do art. 49 do Código de Defesa do Consumidor, o CONTRATANTE pode desistir da " +
                "contratação em até 7 (sete) dias, na forma prevista em lei.\n" +
                "6.2. Exercido validamente o arrependimento no prazo legal, os valores pagos nesse período são " +
                "integralmente restituídos.\n" +
                "6.3. O arrependimento não se confunde com o cancelamento ordinário."),
            new Clausula("7. Alteração ou Migração de Plano",
                "7.1. Migração para outro plano disponível a qualquer momento, conforme condições comerciais " +
                "vigentes.\n" +
                "7.2. Diferenças de valor poderão ser calculadas proporcionalmente ao período de utilização.\n" +
                "7.3. Funcionalidades e limites do novo plano valem a partir da efetivação da troca."),
            new Clausula("8. Disponibilidade e Manutenção",
                "8.1. A CONTRATADA envida esforços razoáveis para manter a plataforma disponível.\n" +
                "8.2. Podem ocorrer indisponibilidades por manutenção, atualizações, falhas de infraestrutura " +
                "ou de terceiros, conectividade, força maior ou caso fortuito.\n" +
                "8.3. Manutenções programadas relevantes serão comunicadas previamente quando possível."),
            new Clausula("9. Responsabilidades do Contratante",
                "9.1. Manter dados cadastrais corretos e atualizados.\n" +
                "9.2. Responder pela utilização adequada da plataforma e pelo conteúdo inserido em sua conta.\n" +
                "9.3. Manter sigilo das credenciais de acesso.\n" +
                "9.4. Não utilizar a plataforma para fins ilícitos, fraudulentos ou que violem direitos de " +
                "terceiros — detalhamento adicional nos Termos de Uso."),
            new Clausula("10. Dados e Responsabilidade pelas Informações",
                "10.1. Os dados inseridos permanecem sob responsabilidade do CONTRATANTE quanto a origem, " +
                "legitimidade e exatidão.\n" +
                "10.2. Ao inserir dados pessoais de clientes/terceiros, o CONTRATANTE deve observar a " +
                "legislação de proteção de dados aplicável.\n" +
                "10.3. A CONTRATADA trata os dados necessários à execução do serviço, segurança, suporte e " +
                "cumprimento de obrigações legais, conforme a Política de Privacidade."),
            new Clausula("11. Proteção de Dados Pessoais — LGPD",
                "11.1. Tratamento de dados pessoais conforme a Lei nº 13.709/2018 (LGPD).\n" +
                "11.2. Bases legais aplicáveis: execução de contrato e cumprimento de obrigação legal/" +
                "regulatória, entre outras previstas na LGPD.\n" +
                "11.3. Medidas técnicas e administrativas razoáveis de segurança são adotadas.\n" +
                "11.4. Dados pessoais não são comercializados para terceiros.\n" +
                "11.5. Direitos dos titulares exercíveis pelos canais oficiais de atendimento.\n" +
                "11.6. Detalhamento completo na Política de Privacidade, que integra este contrato."),
            new Clausula("12. Integrações e Serviços de Terceiros",
                "12.1. A plataforma pode disponibilizar integrações com sistemas e provedores externos.\n" +
                "12.2. O funcionamento dessas integrações depende da disponibilidade e condições dos " +
                "respectivos terceiros.\n" +
                "12.3. Indisponibilidade/alteração de serviço de terceiro pode afetar funcionalidades, sem " +
                "implicar necessariamente falha da CONTRATADA."),
            new Clausula("13. Propriedade Intelectual",
                "13.1. A plataforma, software, marca, layout e demais elementos pertencem à CONTRATADA ou a " +
                "terceiros titulares.\n" +
                "13.2. A contratação concede licença de uso limitada, não exclusiva, pessoal e intransferível, " +
                "durante a vigência da contratação.\n" +
                "13.3. Vedada cópia, engenharia reversa ou exploração indevida da plataforma, salvo autorização " +
                "legal ou da CONTRATADA."),
            new Clausula("14. Limitações de Uso",
                "14.1. Condutas vedadas (atividades ilícitas, acesso não autorizado, código malicioso, " +
                "violação de direitos de terceiros, entre outras) detalhadas nos Termos de Uso.\n" +
                "14.2. A CONTRATADA pode adotar medidas técnicas de segurança, observados os direitos do " +
                "CONTRATANTE."),
            new Clausula("15. Encerramento da Conta e Dados",
                "15.1. Após o cancelamento, a conta segue disponível pelo período já pago.\n" +
                "15.2. Após o encerramento definitivo, os dados podem ser excluídos, anonimizados ou mantidos " +
                "pelo prazo legal necessário.\n" +
                "15.3. Quando tecnicamente disponível, poderá haver exportação de dados antes do encerramento."),
            new Clausula("16. Comunicações",
                "16.1. Comunicações via e-mail cadastrado, notificações na plataforma, WhatsApp ou outros " +
                "canais oficiais.\n" +
                "16.2. O CONTRATANTE deve manter seus dados de contato atualizados."),
            new Clausula("17. Alterações Deste Contrato",
                "17.1. A CONTRATADA pode atualizar este contrato por razões legais, regulatórias, técnicas ou " +
                "comerciais.\n" +
                "17.2. Alterações relevantes serão comunicadas pelos canais da plataforma.\n" +
                "17.3. A continuidade de uso após a vigência das alterações, quando aplicável, implica ciência " +
                "das novas condições, sem prejuízo dos direitos legais do CONTRATANTE."),
            new Clausula("18. Disposições Gerais",
                "18.1. Tolerância quanto a descumprimento não implica renúncia às condições contratuais.\n" +
                "18.2. Disposição inválida não prejudica as demais.\n" +
                "18.3. Este contrato deve ser interpretado junto aos Termos de Uso e à Política de Privacidade."),
            new Clausula("19. Foro e Legislação Aplicável",
                "19.1. Regido pelas leis da República Federativa do Brasil.\n" +
                "19.2. Controvérsias observam o foro competente nos termos da legislação aplicável, " +
                "especialmente as normas de proteção ao consumidor quando aplicáveis."),
            new Clausula("20. Aceite Eletrônico",
                "20.1. Ao marcar o aceite, criar conta ou concluir a contratação, o CONTRATANTE declara acesso " +
                "e concordância com este contrato.\n" +
                "20.2. O registro eletrônico do aceite é armazenado para comprovação da contratação, contendo " +
                "data, horário, identificação da conta, IP e versão do documento aceita — os mesmos dados " +
                "indicados no cabeçalho deste PDF.")
        );
    }
}
