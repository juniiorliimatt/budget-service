-- liquibase formatted sql

-- changeset budget-service:spending-v2-add-audit-columns context:structure labels:spending
-- comment: A entidade Spending sempre mapeou created_by/updated_by (via @CreatedBy/@LastModifiedBy), mas a tabela nunca teve essas colunas - so nao dava erro porque os testes usam H2 com ddl-auto=create-drop (nunca valida contra um schema pre-existente). ddl-auto=validate (dev/prod) acusa a falta assim que sobe contra Postgres real.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'spending' AND column_name = 'created_by'
ALTER TABLE budget.spending ADD COLUMN created_by VARCHAR(50);
ALTER TABLE budget.spending ADD COLUMN updated_by VARCHAR(50);
