-- liquibase formatted sql

-- changeset budget-service:revenues-add-reference-date context:structure labels:revenues
-- comment: Competencia (mes/ano orcamentario) separada da data real do lancamento - ex.: salario recebido dia 30 que custeia as contas do mes seguinte deve ser contado na competencia do mes seguinte, nao no mes do recebimento. Busca/total passam a filtrar por reference_date, nao mais por date. Backfill iguala a data real (sem distincao previa).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'revenues' AND column_name = 'reference_date'
ALTER TABLE budget.revenues ADD COLUMN reference_date DATE;
UPDATE budget.revenues SET reference_date = date WHERE reference_date IS NULL;
ALTER TABLE budget.revenues ALTER COLUMN reference_date SET NOT NULL;
