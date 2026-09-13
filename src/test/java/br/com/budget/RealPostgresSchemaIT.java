package br.com.budget;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Sobe o contexto Spring inteiro (Liquibase rodando todas as migrations + Hibernate
 * {@code ddl-auto=validate}) contra um Postgres real e descartável, com o mesmo
 * role/schema restrito de produção ({@code budget_service}/{@code budget}, ver
 * {@code testcontainers-init.sql}) — nunca o superusuário do container, e nunca o
 * Postgres de dev compartilhado ({@code workbox-postgres}, porta 7050). Mesmo padrão do
 * {@code RealPostgresSchemaIT} do workbox-api.
 *
 * <p>Container novo e descartável a cada execução (sem reuse) é o que garante zero
 * poluição de tabelas entre rodadas de teste — não o nome do schema em si: os
 * changesets deste serviço têm {@code budget.} cravado no SQL bruto (não usam a
 * abstração de schema do Liquibase), então renomear o schema só moveria as tabelas de
 * bookkeeping do próprio Liquibase, nunca as tabelas de negócio, e reescrever
 * changesets já aplicados pra parametrizar o nome quebraria o checksum deles em
 * produção. Isolamento real vem de nunca compartilhar o container entre execuções.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class RealPostgresSchemaIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
            .withDatabaseName("workbox")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("testcontainers-init.sql");

    @DynamicPropertySource
    static void datasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/workbox"
                .formatted(POSTGRES.getHost(), POSTGRES.getMappedPort(5432)));
        registry.add("spring.datasource.username", () -> "budget_service");
        registry.add("spring.datasource.password", () -> "budget_service");
    }

    @Test
    void applicationContextLoadsAgainstRealPostgresSchema() {
        // O teste é o próprio carregamento do contexto: se Liquibase e o mapeamento JPA
        // divergirem (inclusive as tabelas _aud do Envers), ddl-auto=validate falha o
        // boot antes de chegar aqui.
    }
}
