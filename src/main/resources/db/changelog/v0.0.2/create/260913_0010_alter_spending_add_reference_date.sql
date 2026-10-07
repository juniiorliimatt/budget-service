-- liquibase formatted sql

-- changeset budget-service:spending-add-reference-date context:structure labels:spending
-- comment: Competencia (mes/ano orcamentario) separada da data real do lancamento - mesma motivacao de revenues.reference_date. Busca/total/regra 50-30-20 passam a filtrar por reference_date, nao mais por date. Backfill iguala a data real (sem distincao previa).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'spending' AND column_name = 'reference_date'
ALTER TABLE budget.spending ADD COLUMN reference_date DATE;
UPDATE budget.spending SET reference_date = date WHERE reference_date IS NULL;
ALTER TABLE budget.spending ALTER COLUMN reference_date SET NOT NULL;
