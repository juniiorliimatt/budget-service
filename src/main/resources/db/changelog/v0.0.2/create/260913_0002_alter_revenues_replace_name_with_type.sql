-- liquibase formatted sql

-- changeset budget-service:revenues-v2-add-type-id context:structure labels:revenues
-- comment: Adiciona revenue_type_id, nullable ate o backfill do changeset seguinte.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'revenues' AND column_name = 'revenue_type_id'
ALTER TABLE budget.revenues ADD COLUMN revenue_type_id UUID;

-- changeset budget-service:revenues-v2-backfill-type context:data labels:revenues
-- comment: Cria um tipo de receita por nome distinto ja existente e associa cada linha ao tipo correspondente.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM budget.revenues WHERE revenue_type_id IS NOT NULL
INSERT INTO budget.revenue_types (id, name, created_at, created_by)
SELECT gen_random_uuid(), r.name, NOW(), 'system'
FROM (SELECT DISTINCT name FROM budget.revenues) r;

UPDATE budget.revenues r
SET revenue_type_id = rt.id
FROM budget.revenue_types rt
WHERE rt.name = r.name;

-- changeset budget-service:revenues-v2-finalize-type context:structure labels:revenues
-- comment: NOT NULL + FK pro tipo, remove a coluna name antiga.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'revenues' AND column_name = 'name'
ALTER TABLE budget.revenues ALTER COLUMN revenue_type_id SET NOT NULL;
ALTER TABLE budget.revenues ADD CONSTRAINT fk_revenues_revenue_type FOREIGN KEY (revenue_type_id) REFERENCES budget.revenue_types (id);
ALTER TABLE budget.revenues DROP COLUMN name;
