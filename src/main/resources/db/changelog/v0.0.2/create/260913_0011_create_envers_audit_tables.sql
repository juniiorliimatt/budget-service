-- liquibase formatted sql

-- changeset budget-service:envers-v1-rev-info context:structure labels:audit
-- comment: Tabela de revisões do Hibernate Envers, compartilhada entre todas as entidades @Audited. Colunas id/timestamp são o mapeamento real de DefaultRevisionEntity (int id, long timestamp - sem override de nome). @GeneratedValue sem estratégia explícita resolve pra SEQUENCE no Postgres (Hibernate 6), convenção de nome "{table}_seq" - não IDENTITY. username vem do RevisionListenerImpl (SecurityContext). Mesmo padrão do workbox-api (db/changelog/v0.0.2/create/260829_0003_create_envers_audit_tables.sql).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'rev_info'
-- INCREMENT BY 50 é o allocationSize default do JPA/Hibernate pra @GeneratedValue sem
-- @SequenceGenerator explícito - tem que bater exatamente com o que o Hibernate espera.
CREATE SEQUENCE budget.rev_info_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE budget.rev_info
(
    id        INTEGER PRIMARY KEY DEFAULT nextval('budget.rev_info_seq'),
    timestamp BIGINT,
    username  VARCHAR(255) NOT NULL
);

-- changeset budget-service:envers-v1-revenue-types-aud context:structure labels:audit
-- comment: Histórico de revisões de revenue_types (Hibernate Envers @Audited) - espelha as colunas da tabela principal. rev/revtype são os nomes default do Envers pra FK de revisão.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'revenue_types_aud'
CREATE TABLE budget.revenue_types_aud
(
    id         UUID         NOT NULL,
    rev        INTEGER      NOT NULL REFERENCES budget.rev_info (id),
    revtype    SMALLINT,
    name       VARCHAR(50),
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    PRIMARY KEY (id, rev)
);

-- changeset budget-service:envers-v1-spending-types-aud context:structure labels:audit
-- comment: Histórico de revisões de spending_types (Hibernate Envers @Audited) - espelha as colunas da tabela principal, incluindo category (regra 50/30/20).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'spending_types_aud'
CREATE TABLE budget.spending_types_aud
(
    id         UUID         NOT NULL,
    rev        INTEGER      NOT NULL REFERENCES budget.rev_info (id),
    revtype    SMALLINT,
    name       VARCHAR(50),
    category   VARCHAR(20),
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    PRIMARY KEY (id, rev)
);

-- changeset budget-service:envers-v1-revenues-aud context:structure labels:audit
-- comment: Histórico de revisões de revenues (Hibernate Envers @Audited) - espelha as colunas da tabela principal, incluindo a associação com revenue_type (não @NotAudited, faz parte do que se quer rastrear).
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'revenues_aud'
CREATE TABLE budget.revenues_aud
(
    id              UUID         NOT NULL,
    rev             INTEGER      NOT NULL REFERENCES budget.rev_info (id),
    revtype         SMALLINT,
    revenue_type_id UUID,
    value           NUMERIC(10, 2),
    date            DATE,
    reference_date  DATE,
    owner_username  VARCHAR(255),
    created_at      TIMESTAMP(6),
    updated_at      TIMESTAMP(6),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    PRIMARY KEY (id, rev)
);

-- changeset budget-service:envers-v1-spending-aud context:structure labels:audit
-- comment: Histórico de revisões de spending (Hibernate Envers @Audited) - espelha as colunas da tabela principal, incluindo a associação com spending_type.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'spending_aud'
CREATE TABLE budget.spending_aud
(
    id               UUID         NOT NULL,
    rev              INTEGER      NOT NULL REFERENCES budget.rev_info (id),
    revtype          SMALLINT,
    spending_type_id UUID,
    description      VARCHAR(250),
    value            NUMERIC(7, 2),
    date             DATE,
    reference_date   DATE,
    was_paid         BOOLEAN,
    owner_username   VARCHAR(255),
    created_at       TIMESTAMP(6),
    updated_at       TIMESTAMP(6),
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50),
    PRIMARY KEY (id, rev)
);
