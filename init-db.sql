-- Script de inicialização dos bancos de dados
-- Executado automaticamente pelo PostgreSQL na primeira inicialização

CREATE DATABASE db_catalog;
CREATE DATABASE db_table;
CREATE DATABASE db_order;

GRANT ALL PRIVILEGES ON DATABASE db_catalog TO comanda;
GRANT ALL PRIVILEGES ON DATABASE db_table TO comanda;
GRANT ALL PRIVILEGES ON DATABASE db_order TO comanda;
