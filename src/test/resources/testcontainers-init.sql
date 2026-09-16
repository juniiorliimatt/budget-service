-- Espelha initdb/*.sql da raiz do monorepo (só a parte de budget_service — este
-- container não roda workbox-api). Roda como o superusuário do container
-- (POSTGRES_USER/POSTGRES_PASSWORD do PostgreSQLContainer), igual em produção.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE ROLE budget_service WITH LOGIN PASSWORD 'budget_service';
GRANT CONNECT ON DATABASE workbox TO budget_service;
CREATE SCHEMA IF NOT EXISTS budget AUTHORIZATION budget_service;
