-- Nova permissão (CONFIG_TAXAS_MAQUININHA) — concede automaticamente pros
-- grupos "Administrador" padrão já existentes, já que esse grupo representa
-- acesso total (mesma lógica de GrupoService.criarGruposPadrao).
INSERT INTO grupo_permissoes (grupo_id, permissao)
SELECT id, 'CONFIG_TAXAS_MAQUININHA' FROM grupos
WHERE nome = 'Administrador' AND padrao = true
  AND id NOT IN (
    SELECT grupo_id FROM grupo_permissoes WHERE permissao = 'CONFIG_TAXAS_MAQUININHA'
  );
