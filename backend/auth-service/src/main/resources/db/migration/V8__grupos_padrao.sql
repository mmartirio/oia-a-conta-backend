-- Grupos padrão (Administrador/Garçom/Cozinheiro/Entregador) pré-criados pra
-- cada restaurante já existente — ponto de partida pronto pra uso, sem
-- excluir a criação de grupos customizados. Não podem ser excluídos
-- (padrao=true, ver GrupoService.excluir). Restaurantes novos ganham esses
-- mesmos 4 grupos via GrupoService.criarGruposPadrao, chamado no cadastro.
ALTER TABLE grupos ADD COLUMN padrao BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
DECLARE
    r RECORD;
    novo_grupo_id BIGINT;
BEGIN
    FOR r IN SELECT id FROM restaurantes LOOP
        INSERT INTO grupos (restaurante_id, nome, padrao) VALUES (r.id, 'Administrador', true) RETURNING id INTO novo_grupo_id;
        INSERT INTO grupo_permissoes (grupo_id, permissao) VALUES
            (novo_grupo_id, 'DASHBOARD'), (novo_grupo_id, 'CARDAPIO'), (novo_grupo_id, 'MESAS'),
            (novo_grupo_id, 'COZINHA'), (novo_grupo_id, 'COMANDA'), (novo_grupo_id, 'GARCOM'),
            (novo_grupo_id, 'DELIVERY'), (novo_grupo_id, 'CAIXA_PDV'), (novo_grupo_id, 'ENTREGADOR'),
            (novo_grupo_id, 'USUARIOS'), (novo_grupo_id, 'FINANCEIRO'),
            (novo_grupo_id, 'WHATSAPP_CONEXAO'), (novo_grupo_id, 'WHATSAPP_MENSAGENS'), (novo_grupo_id, 'WHATSAPP_CONVERSAS'),
            (novo_grupo_id, 'SUPORTE'),
            (novo_grupo_id, 'CONFIG_STATUS_LOJA'), (novo_grupo_id, 'CONFIG_ALERTA_PEDIDO'), (novo_grupo_id, 'CONFIG_DADOS_EMPRESA'),
            (novo_grupo_id, 'CONFIG_PIX'), (novo_grupo_id, 'CONFIG_COMISSOES'), (novo_grupo_id, 'CONFIG_LOGO'),
            (novo_grupo_id, 'CONFIG_CORES'), (novo_grupo_id, 'CONFIG_BACKGROUND'), (novo_grupo_id, 'CONFIG_HORARIOS'),
            (novo_grupo_id, 'CONFIG_PAUSAS');

        INSERT INTO grupos (restaurante_id, nome, padrao) VALUES (r.id, 'Garçom', true) RETURNING id INTO novo_grupo_id;
        INSERT INTO grupo_permissoes (grupo_id, permissao) VALUES
            (novo_grupo_id, 'GARCOM'), (novo_grupo_id, 'COMANDA'), (novo_grupo_id, 'DELIVERY');

        INSERT INTO grupos (restaurante_id, nome, padrao) VALUES (r.id, 'Cozinheiro', true) RETURNING id INTO novo_grupo_id;
        INSERT INTO grupo_permissoes (grupo_id, permissao) VALUES (novo_grupo_id, 'COZINHA');

        INSERT INTO grupos (restaurante_id, nome, padrao) VALUES (r.id, 'Entregador', true) RETURNING id INTO novo_grupo_id;
        INSERT INTO grupo_permissoes (grupo_id, permissao) VALUES (novo_grupo_id, 'ENTREGADOR');
    END LOOP;
END $$;
