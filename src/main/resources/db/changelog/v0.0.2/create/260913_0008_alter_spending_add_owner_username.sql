-- liquibase formatted sql

-- changeset budget-service:spending-add-owner-username context:structure labels:spending
-- comment: Cada Spending passa a pertencer a um unico usuario logado (owner_username = subject retornado pela introspeccao do workbox-api) - antes os lancamentos eram compartilhados entre todos os usuarios autenticados.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'spending' AND column_name = 'owner_username'
ALTER TABLE budget.spending ADD COLUMN owner_username VARCHAR(255);
UPDATE budget.spending SET owner_username = COALESCE(created_by, 'system') WHERE owner_username IS NULL;
ALTER TABLE budget.spending ALTER COLUMN owner_username SET NOT NULL;
