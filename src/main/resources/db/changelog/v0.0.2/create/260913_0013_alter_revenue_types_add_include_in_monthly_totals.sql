-- liquibase formatted sql

-- changeset budget-service:revenue-types-add-include-in-monthly-totals context:structure labels:revenue_types
-- comment: Controla se o tipo entra no total mensal "de tudo" (resumo mensal, regra 50/30/20) - ex.: saldo que sobra de dezembro e e lancado em janeiro pra fechar o ano; contar em janeiro infla o mes com dinheiro que nao e receita nova daquele mes. Conta normalmente no anual e no by-type (governados por include_in_totals, flag independente). Backfill true pras linhas ja existentes (comportamento anterior, todo tipo contava em qualquer total).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'revenue_types' AND column_name = 'include_in_monthly_totals'
ALTER TABLE budget.revenue_types ADD COLUMN include_in_monthly_totals BOOLEAN;
UPDATE budget.revenue_types SET include_in_monthly_totals = true WHERE include_in_monthly_totals IS NULL;
ALTER TABLE budget.revenue_types ALTER COLUMN include_in_monthly_totals SET NOT NULL;

-- changeset budget-service:revenue-types-aud-add-include-in-monthly-totals context:structure labels:audit
-- comment: Espelha include_in_monthly_totals na tabela de historico do Envers (revenue_types_aud).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'budget' AND table_name = 'revenue_types_aud' AND column_name = 'include_in_monthly_totals'
ALTER TABLE budget.revenue_types_aud ADD COLUMN include_in_monthly_totals BOOLEAN;
