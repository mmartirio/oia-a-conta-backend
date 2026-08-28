-- Links de redes sociais exibidos no rodapé da landing page pública,
-- gerenciados pelo painel Gestor (SUPER_ADMIN).
CREATE TABLE links_sociais (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('INSTAGRAM', 'WHATSAPP')),
    url VARCHAR(500) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
