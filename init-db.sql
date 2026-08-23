-- Script de inicialização dos bancos de dados
-- Executado automaticamente pelo PostgreSQL na primeira inicialização

CREATE DATABASE db_catalog;
CREATE DATABASE db_table;
CREATE DATABASE db_order;
CREATE DATABASE db_billing;
CREATE DATABASE db_whatsapp;
CREATE DATABASE db_ifood;

GRANT ALL PRIVILEGES ON DATABASE db_catalog  TO oiaconta;
GRANT ALL PRIVILEGES ON DATABASE db_table    TO oiaconta;
GRANT ALL PRIVILEGES ON DATABASE db_order    TO oiaconta;
GRANT ALL PRIVILEGES ON DATABASE db_billing  TO oiaconta;
GRANT ALL PRIVILEGES ON DATABASE db_whatsapp TO oiaconta;
GRANT ALL PRIVILEGES ON DATABASE db_ifood    TO oiaconta;
