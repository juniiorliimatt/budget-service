-- liquibase formatted sql

-- changeset budget-service:spending_types-v2-add-category context:structure labels:spending_types
-- comment: Classificacao da regra 50/30/20 (ESSENTIAL/PERSONAL/SAVINGS) pertence ao tipo de despesa, nao ao lancamento individual. Backfill PERSONAL pras linhas ja existentes (poucas, todas criadas em teste manual) - reclassificar via PUT /api/v1/spending-types/{id} depois.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'spending_types' AND column_name = 'category'
ALTER TABLE budget.spending_types ADD COLUMN category VARCHAR(20);
UPDATE budget.spending_types SET category = 'PERSONAL' WHERE category IS NULL;
ALTER TABLE budget.spending_types ALTER COLUMN category SET NOT NULL;
