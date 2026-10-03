# budget-service — instruções do serviço

> Complementa o [`CLAUDE.md` da raiz](../CLAUDE.md) (visão do monorepo, contrato, commits,
> infra) e as regras globais de `~/.claude/CLAUDE.md`. Aqui só o que é específico deste
> serviço. Detalhes de uso/endpoints: [`README.md`](README.md).

## Papel e autoria
- Domínio de **finanças pessoais** (receitas, despesas, tipos, regra 50/30/20). *Resource
  server*: não emite token nem tem login — valida o Bearer do `workbox-api` por
  introspecção remota. Foi o **primeiro serviço** no padrão "um repo por microserviço" e
  é a referência de estrutura pros próximos.
- Autoria: **padrão do monorepo é o desenvolvedor implementar e o Claude só aconselhar**,
  mas aqui há **permissão total temporária** concedida pelo desenvolvedor (Claude
  implementa direto). Detalhes e limites em [raiz → Divisão de
  responsabilidade](../CLAUDE.md#divisão-de-responsabilidade); em dúvida se ainda vale,
  perguntar.

## Stack e execução
- Java 25 LTS, Spring Boot 3.5.16, Gradle 9.7.1 (`./gradlew`), Spring Data JPA +
  Liquibase, Hibernate Envers, Spring Security 6 (OAuth2 resource server, **opaque
  token**), springdoc, JaCoCo, Sonar, Lombok.
- Porta **7052** (container 8081). Profiles: `dev` (default, Postgres `:7050`), `prod`,
  `test` (H2). Role Postgres `budget_service`, schema **`budget`** — sem acesso ao schema
  do `workbox-api`.
- Comandos: `./gradlew bootRun`, `./gradlew check`, `./gradlew generateOpenApiDocs`.

## Estrutura (`br.com.budget`)
`config/` (OpenAPI, `SecurityConfig`, `WorkboxTokenIntrospector`, `AuditorAwareImpl`,
`MessageConfig`, Envers em `audit/`) · `controllers/` (`Revenue`, `Spending`,
`RevenueType`, `SpendingType`, `BudgetRule`) · `exceptions/` (+ `handler/`) ·
`models/{dto, entities, enums}` · `repositories/` · `services/` (inclui `AuditService`).
Rotas (todas `/api/v1`): `revenues`, `spendings`, `revenue-types`, `spending-types`
(CRUD + `/{id}/history` do Envers; receitas/despesas também `/total`, `/by-type`,
`/batch`, `/batch/annual`) e `budget-rules` (`/fifty-thirty-twenty`, `/monthly-summary`,
`/yearly-summary`). O contrato completo é o `openapi/openapi.yaml`.

## Regras de domínio e armadilhas
- **Escopo por dono**: toda `Revenue`/`Spending` tem `owner_username` (do usuário
  autenticado, **nunca do payload**); consultas sempre filtradas por ele. Mantenha o
  filtro em qualquer query/endpoint novo (IDOR — OWASP API1).
- **Tipos são catálogo**: `name` de receita/despesa é referência a `RevenueType`/
  `SpendingType` (não texto livre). `SpendingType.category` ∈ `ESSENTIAL | PERSONAL |
  SAVINGS` (regra 50/30/20 — `SpendingCategory`).
- **Flags de `RevenueType`** (ambas default `true`, não afetam CRUD, busca nem total de um
  tipo específico via `?typeId=`):
  - `includeInTotals=false` → fora do agrupamento por tipo (`/revenues/by-type`) e do
    total anual "de tudo" (`yearly-summary`). Ex.: "Caixinha" (sobra recolocada como
    receita; já contada no "Salário" — contar nos dois duplicaria).
  - `includeInMonthlyTotals=false` → fora do total mensal "de tudo" (resumo mensal /
    50-30-20). Ex.: saldo de dezembro lançado em janeiro: não é receita nova do mês, mas
    conta no anual.
- Datas: `date` (lançamento) vs `referenceDate` (competência) — filtros `month`/`year`
  usam a competência; não trocar uma pela outra. Valores monetários em `BigDecimal`.
- **Auditoria**: `@CreatedBy/@CreatedDate/...` em toda entidade + Envers (`@Audited`,
  `*_aud`). Entidade nova segue o mesmo padrão e ganha tabela de auditoria no changeset.
- **Autenticação**: `WorkboxTokenIntrospector` chama
  `POST /api/v1/auth/introspect` (HTTP Basic com `INTROSPECTION_CLIENT_ID/SECRET`, que
  precisam bater com uma linha ativa em `workbox.api_clients`). A claim `roles` já vem
  `ROLE_*` e vira authority sem prefixo adicional. Nunca decodificar JWT localmente nem
  conhecer `JWT_SECRET`. Referência: [`docs/budget-service-migracao-introspeccao.md`](../docs/budget-service-migracao-introspeccao.md).
- **CORS**: `cors.allowed-origins` (default `http://localhost:7053`) via Spring Security
  nativo; origem específica ecoada (nunca `*`) com credenciais.
- **Liquibase**: `includeAll` em `db/changelog/v0.0.1/create` e `v0.0.2/create`; arquivo
  novo `yymmdd_nnnn_<acao>_<alvo>.sql` em `v0.0.2/create`. **Nunca editar changeset já
  aplicado.** Backward-compatible (expand → migrate → contract); proibido `SELECT *`.
- `springdoc.writer-with-order-by-keys=true` — não remover (evita diff falso no CI).

## Convenção Java deste repo
- **`final` obrigatório** em todo parâmetro e variável local (`src/main` e `src/test`),
  exceto reatribuição real. Código novo já nasce conforme.
- Lombok permitido (já nas dependências). Javadoc e nomes de métodos de negócio da camada
  de serviço em **português** (padrão atual).

## Testes (test-first)
- JUnit 5 + Spring Boot Test + MockMvc `@WebMvcTest` (serviços via `@MockitoBean`), auth
  simulada com `SecurityMockMvcRequestPostProcessors.opaqueToken()` — **não** `jwt()`.
- `RealPostgresSchemaIT` (Testcontainers, **exige Docker**): contexto inteiro contra
  Postgres descartável com o role/schema restritos de produção — pega drift entre entidade
  JPA e Liquibase que o H2 `create-drop` não reproduz.
- Cucumber está nas dependências (mesma versão do `workbox-api`) mas **ainda não há
  `.feature`/steps** — ao criar, seguir o padrão do `workbox-api` (feature primeiro).
- Testes de controller + ITs com Testcontainers: `RealPostgresSchemaIT` e `TotalPorTipoIT`
  (`totalPorTipo` anual/mensal: competência, dono e flags de `RevenueType`). Lógica nova de
  service/cálculo (totais, 50/30/20) deve nascer com teste unitário/IT.
- `GET /budget-rules/monthly-series?year` devolve os 12 meses (receita + despesas realizadas por
  categoria 50/30/20) em 2 queries agrupadas; `MonthlySeriesIT` exige os **mesmos números** de
  `fifty-thirty-twenty` mês a mês (competência, dono, `includeInMonthlyTotals`).
- Query param obrigatório ausente ou com tipo errado responde **400** (`RestExceptionHandler`), nunca
  500 — mantenha ao criar handlers.
- `GET /revenues|spendings/by-type` aceita `month` opcional: sem ele, ano inteiro (receitas
  respeitam `includeInTotals`); com ele, só o mês (receitas respeitam `includeInMonthlyTotals`,
  mesma regra do total mensal).

## Contrato (OpenAPI)
`openapi/openapi.yaml` é a fonte da verdade e o front consome só dele. Mudou rota/DTO/
status/auth → regenerar (`./gradlew generateOpenApiDocs`) e commitar na mesma mudança (CI
`contract-drift-check`); ajustar `workbox-app` na mesma tarefa quando o contrato
observável mudar (ver raiz). O proxy do front roteia `revenues|spendings|revenue-types|
spending-types|budget-rules` pra cá (`vite.config.ts` e `nginx.conf.template`) — rota
nova com prefixo diferente exige ajustar os dois.

## Commits
pt-BR, Conventional Commits, conforme o
[CLAUDE.md da raiz](../CLAUDE.md#convenção-de-mensagens-de-commit). Trabalhar em
`develop`; push só com confirmação.
