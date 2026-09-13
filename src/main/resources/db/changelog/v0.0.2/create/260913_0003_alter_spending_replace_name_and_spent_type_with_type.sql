-- liquibase formatted sql

-- changeset budget-service:spending-v2-add-type-id context:structure labels:spending
-- comment: Adiciona spending_type_id, nullable ate o backfill do changeset seguinte.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'spending' AND column_name = 'spending_type_id'
ALTER TABLE budget.spending ADD COLUMN spending_type_id UUID;

-- changeset budget-service:spending-v2-backfill-type context:data labels:spending
-- comment: Cria um tipo de despesa por nome distinto ja existente e associa cada linha. O antigo spent_type (ESSENTIAL/PERSONAL/SAVINGS) era um eixo de classificacao separado, descartado nesta migracao - nao entra no backfill do tipo novo.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM budget.spending WHERE spending_type_id IS NOT NULL
INSERT INTO budget.spending_types (id, name, created_at, created_by)
SELECT gen_random_uuid(), s.name, NOW(), 'system'
FROM (SELECT DISTINCT name FROM budget.spending) s;

UPDATE budget.spending sp
SET spending_type_id = st.id
FROM budget.spending_types st
WHERE st.name = sp.name;

-- changeset budget-service:spending-v2-finalize-type context:structure labels:spending
-- comment: NOT NULL + FK pro tipo, remove as colunas name e spent_type antigas.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'spending' AND column_name = 'name'
ALTER TABLE budget.spending ALTER COLUMN spending_type_id SET NOT NULL;
ALTER TABLE budget.spending ADD CONSTRAINT fk_spending_spending_type FOREIGN KEY (spending_type_id) REFERENCES budget.spending_types (id);
ALTER TABLE budget.spending DROP COLUMN name;
ALTER TABLE budget.spending DROP COLUMN spent_type;
