--liquibase formatted sql

-- changeset budget-service:create-schema-budget context:structure labels:schema
-- comment: Cria o schema budget (banco isolado por serviço)
CREATE SCHEMA IF NOT EXISTS budget;
