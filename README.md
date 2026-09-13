# budget-service

Microserviço de estudo (finanças pessoais) do [monorepo `workbox`](../README.md) —
**resource server**: valida os access tokens emitidos pelo [`workbox-api`](../workbox-api/README.md)
via introspecção remota (client credentials), sem fluxo de login próprio e sem conhecer
nenhum segredo de assinatura de JWT. Primeiro serviço a seguir o padrão "um repo/submodule
por microserviço" — serve de referência pros próximos.

Também espelhado no [GitHub](https://github.com/juniiorliimatt/budget-service) — todo
push pro GitLab é replicado automaticamente via git hook. Ver
[README raiz](../README.md#espelho-no-github--git-hooks).

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / runtime | Java 25 LTS (toolchain Gradle) |
| Framework | Spring Boot 3.5.16 |
| Build | Gradle 9.7.1 |
| Persistência | Spring Data JPA + Hibernate, Liquibase (migrations), schema `budget` próprio |
| Auditoria | `@CreatedBy`/`@CreatedDate`/etc. (Spring Data JPA) em toda entidade + Hibernate Envers (`@Audited`) com histórico completo de revisões — mesmo padrão do `workbox-api` |
| Banco | PostgreSQL (dev/prod), H2 em memória (test) |
| Segurança | Spring Security 6 (OAuth2 resource server, opaque token), valida token via introspecção remota no workbox-api |
| Documentação de API | springdoc-openapi (Swagger UI + contrato versionado) |
| Cobertura | JaCoCo |

## Estrutura de pacotes

```
br.com.budget
├── config/            OpenAPI, Security (resource server), JPA auditing
├── controllers/       RevenueController, SpendingController, RevenueTypeController,
│                      SpendingTypeController, BudgetRuleController
├── exceptions/        Exceções de domínio + handler global (RestExceptionHandler)
├── models/
│   ├── dto/           DTOs de entrada/saída
│   ├── entities/       Revenue, Spending, RevenueType, SpendingType
│   └── enums/          SpendingCategory (ESSENTIAL/PERSONAL/SAVINGS — regra 50/30/20)
├── repositories/      Spring Data JPA
└── services/          RevenueService, SpendingService, RevenueTypeService,
                       SpendingTypeService, BudgetRuleService
```

`RevenueType`/`SpendingType` são catálogos com CRUD próprio (`name` de receita/despesa
deixou de ser texto livre em `Revenue`/`Spending` — agora é uma referência pro tipo
cadastrado). `SpendingType` carrega também a `category` da regra 50/30/20.

## Autenticação

Este serviço **não emite tokens** — confia nos access tokens emitidos por
`POST /api/v1/auth/login` no `workbox-api`, mas **não os decodifica localmente**: valida
cada um via introspecção remota (`POST /api/v1/auth/introspect` no `workbox-api`, com
client credentials HTTP Basic — `INTROSPECTION_CLIENT_ID`/`INTROSPECTION_CLIENT_SECRET`,
tem que bater com uma linha ativa em `workbox.api_clients`). Isso propaga revogação
(logout/troca de senha) de forma automática — o que uma decodificação local nunca
enxergaria. Peça um token no workbox-api e mande em `Authorization: Bearer <token>`
aqui, como sempre. A claim `roles` do resultado da introspecção vira authority
diretamente (sem prefixo adicional, já vem `ROLE_*` do emissor). Ver
[`docs/budget-service-migracao-introspeccao.md`](../docs/budget-service-migracao-introspeccao.md)
na raiz pra detalhes de implementação.

## Rodando localmente

Profiles disponíveis (`spring.profiles.active`):

| Profile | Banco | Uso |
|---|---|---|
| `test` | H2 em memória (`ddl-auto=create-drop`) | Testes automatizados, geração do contrato OpenAPI |
| `dev` (default) | PostgreSQL local via `DATABASE_URL` (default `jdbc:postgresql://localhost:5433/workbox`), schema `budget` | Desenvolvimento |
| `prod` | PostgreSQL via `DATABASE_URL` (obrigatório) | Deploy |

```bash
./gradlew bootRun                                          # profile dev, exige Postgres local
./gradlew bootRun --args='--spring.profiles.active=test'   # sem dependência externa
```

Sobe em `PORT` (default **8081** — evita colidir com o `workbox-api`, que usa 8080, ao
rodar os dois juntos localmente).

Postgres local sobe via `docker-compose.yml` na raiz do monorepo (ver [README
raiz](../README.md#rodando-localmente)) na porta **5433**, não 5432 — passe
`DATABASE_URL=jdbc:postgresql://localhost:5433/workbox`. Banco único (`workbox`)
compartilhado com o `workbox-api` — este serviço só enxerga o schema `budget`, via o
role `budget_service` (default de `POSTGRES_USER`/`POSTGRES_PASSWORD`), sem acesso ao
schema `api` do outro serviço.

CORS: `cors.allowed-origins` (default `http://localhost:5173,http://127.0.0.1:5173`,
mesma origem do `workbox-app` em dev) via Spring Security nativo — não um `Filter`
manual. Origem específica é ecoada (nunca `*`), com `Access-Control-Allow-Credentials:
true`, para funcionar com `withCredentials: true` no cliente HTTP do frontend.

## Contrato de API (OpenAPI)

`openapi/openapi.yaml` é o contrato REST versionado — fonte da verdade para qualquer
client (frontend, agentes de IA). Regenerar:

```bash
./gradlew generateOpenApiDocs
git diff openapi/openapi.yaml
```

A task sobe a aplicação no profile `test`, baixa `/v3/api-docs.yaml` e grava em
`openapi/openapi.yaml`. `springdoc.writer-with-order-by-keys=true` mantém a saída
determinística (sem isso, a ordem dos campos do schema varia entre execuções e o CI
acusa diff falso).

Ver também: [AGENTS.md](../AGENTS.md).

## Convenção de commits

Sempre em português (pt-BR), Conventional Commits com o prefixo de tipo em inglês:

```
<tipo>(<escopo opcional>): <descrição curta e objetiva em português>
```

Tipos aceitos: `feat`, `fix`, `docs`, `chore`, `test`, `refactor`, `style`, `perf`, `ci`,
`revert`. Vale pros quatro repositórios do monorepo — regra completa e exemplo em
[AGENTS.md](../AGENTS.md#convenção-de-mensagens-de-commit).

## Testes

```bash
./gradlew check
```

JUnit 5 + Spring Boot Test + MockMvc, autenticação simulada via
`SecurityMockMvcRequestPostProcessors.jwt()`.

## CI/CD

`.gitlab-ci.yml`: `test` (build + testes) → `contract-drift-check` (contrato em dia) →
`build` (empacota o JAR). `sonarcloud-check` roda análise estática em merge requests e em
pushes diretos à `main` (não `develop` — só dispara em MR ou push na branch protegida).
