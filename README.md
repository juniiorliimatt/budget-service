# budget-service

Microserviço de estudo (finanças pessoais) do [monorepo `workbox`](../README.md) —
**resource server**: valida os JWTs emitidos pelo [`workbox-api`](../workbox-api/README.md)
(segredo HS256 compartilhado), sem fluxo de login próprio. Primeiro serviço a seguir o
padrão "um repo/submodule por microserviço" — serve de referência pros próximos.

Também espelhado no [GitHub](https://github.com/juniiorliimatt/budget-service) — todo
push pro GitLab é replicado automaticamente via git hook. Ver
[README raiz](../README.md#espelho-no-github--git-hooks).

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / runtime | Java 26 (toolchain Gradle) |
| Framework | Spring Boot 3.5.16 |
| Build | Gradle 9.7.1 |
| Persistência | Spring Data JPA + Hibernate, Liquibase (migrations), schema `budget` próprio |
| Banco | PostgreSQL (dev/prod), H2 em memória (test) |
| Segurança | Spring Security 6 (OAuth2 resource server), valida JWT do workbox-api |
| Documentação de API | springdoc-openapi (Swagger UI + contrato versionado) |
| Cobertura | JaCoCo |

## Estrutura de pacotes

```
br.com.budget
├── config/            OpenAPI, Security (resource server), JPA auditing
├── exceptions/         Exceções de domínio + handler global (RestExceptionHandler)
└── revenue/
    ├── controllers/     RevenueController (CRUD REST)
    ├── dto/             DTOs de entrada/saída
    ├── entities/         Revenue
    ├── repositories/     Spring Data JPA
    └── services/         RevenueService
```

## Autenticação

Este serviço **não emite tokens** — ele confia nos JWTs emitidos por
`POST /api/auth/login` no `workbox-api`, validados com o mesmo segredo HS256
(`jwt.secret`/`JWT_SECRET`, mesmo valor default nos dois serviços). Peça um token no
workbox-api e mande em `Authorization: Bearer <token>` aqui. A claim `roles` do JWT vira
authority diretamente (sem prefixo adicional, já vem `ROLE_*` do emissor).

## Rodando localmente

Profiles disponíveis (`spring.profiles.active`):

| Profile | Banco | Uso |
|---|---|---|
| `test` | H2 em memória (`ddl-auto=create-drop`) | Testes automatizados, geração do contrato OpenAPI |
| `dev` (default) | PostgreSQL local via `DATABASE_URL` (default `jdbc:postgresql://localhost:5432/budget`), schema `budget` | Desenvolvimento |
| `prod` | PostgreSQL via `DATABASE_URL` (obrigatório) | Deploy |

```bash
./gradlew bootRun                                          # profile dev, exige Postgres local
./gradlew bootRun --args='--spring.profiles.active=test'   # sem dependência externa
```

Sobe em `PORT` (default **8081** — evita colidir com o `workbox-api`, que usa 8080, ao
rodar os dois juntos localmente).

Postgres local sobe via `docker-compose.yml` na raiz do monorepo (ver [README raiz](../README.md#rodando-localmente))
na porta **5433**, não 5432 — passe `DATABASE_URL=jdbc:postgresql://localhost:5433/budget`.

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

## Testes

```bash
./gradlew check
```

JUnit 5 + Spring Boot Test + MockMvc, autenticação simulada via
`SecurityMockMvcRequestPostProcessors.jwt()`.

## CI/CD

`.gitlab-ci.yml`: `test` → `contract-drift-check` (contrato em dia) → `build`.
