-- liquibase formatted sql

-- changeset budget-service:revenue-types-add-include-in-totals context:structure labels:revenue_types
-- comment: Controla se o tipo entra na soma agrupada por tipo (endpoints by-type) - ex.: "Caixinha" e sobra de salario de mes anterior recolocada como receita, ja contabilizada dentro do proprio "Salario" quando entrou; incluir de novo la duplicaria o valor na tela de metas. Nao afeta o total geral (mes/ano), so o agrupamento por tipo. Backfill true pras linhas ja existentes (comportamento anterior, todo tipo contava).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'revenue_types' AND column_name = 'include_in_totals'
ALTER TABLE budget.revenue_types ADD COLUMN include_in_totals BOOLEAN;
UPDATE budget.revenue_types SET include_in_totals = true WHERE include_in_totals IS NULL;
ALTER TABLE budget.revenue_types ALTER COLUMN include_in_totals SET NOT NULL;

-- changeset budget-service:revenue-types-aud-add-include-in-totals context:structure labels:audit
-- comment: Espelha include_in_totals na tabela de historico do Envers (revenue_types_aud), mesma logica das outras colunas adicionadas depois da tabela _aud original.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'revenue_types_aud' AND column_name = 'include_in_totals'
ALTER TABLE budget.revenue_types_aud ADD COLUMN include_in_totals BOOLEAN;
