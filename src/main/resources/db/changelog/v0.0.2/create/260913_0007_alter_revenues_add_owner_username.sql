-- liquibase formatted sql

-- changeset budget-service:revenues-add-owner-username context:structure labels:revenues
-- comment: Cada Revenue passa a pertencer a um unico usuario logado (owner_username = subject retornado pela introspeccao do workbox-api) - antes os lancamentos eram compartilhados entre todos os usuarios autenticados.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'revenues' AND column_name = 'owner_username'
ALTER TABLE budget.revenues ADD COLUMN owner_username VARCHAR(255);
UPDATE budget.revenues SET owner_username = COALESCE(created_by, 'system') WHERE owner_username IS NULL;
ALTER TABLE budget.revenues ALTER COLUMN owner_username SET NOT NULL;
