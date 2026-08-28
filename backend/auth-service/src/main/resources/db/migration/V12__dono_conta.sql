-- Marca o usuário dono (primeiro ADMIN de cada restaurante) e garante que ele
-- sempre esteja no grupo "Administrador" — ver AuthService.registro() /
-- criarContaFromPendente() e a trava em UsuarioService.atualizar() que impede
-- tirar o dono desse grupo.
ALTER TABLE usuarios ADD COLUMN dono_conta BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
DECLARE
    r RECORD;
    dono_id BIGINT;
    grupo_admin_id BIGINT;
BEGIN
    FOR r IN SELECT id FROM restaurantes LOOP
        SELECT id INTO dono_id FROM usuarios
            WHERE restaurante_id = r.id AND role = 'ADMIN'
            ORDER BY created_at ASC LIMIT 1;
        IF dono_id IS NOT NULL THEN
            SELECT id INTO grupo_admin_id FROM grupos
                WHERE restaurante_id = r.id AND nome = 'Administrador' LIMIT 1;
            UPDATE usuarios SET dono_conta = true,
                grupo_id = COALESCE(grupo_id, grupo_admin_id)
                WHERE id = dono_id;
        END IF;
    END LOOP;
END $$;
