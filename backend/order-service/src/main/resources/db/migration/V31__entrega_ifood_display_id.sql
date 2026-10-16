-- Código curto do pedido no iFood (ex: "3166"), mostrado no painel no lugar
-- do id interno pra pedidos de origem iFood — é o número que aparece pro
-- lojista no Portal do Parceiro/homologação, então precisa bater visualmente.
ALTER TABLE entregas ADD COLUMN ifood_display_id VARCHAR(20);
