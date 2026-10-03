package br.com.budget;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.budget.models.dto.RevenueDTO;
import br.com.budget.models.dto.RevenueTypeDTO;
import br.com.budget.models.dto.SpendingDTO;
import br.com.budget.models.dto.SpendingTypeDTO;
import br.com.budget.models.dto.TypeTotalDTO;
import br.com.budget.models.enums.SpendingCategory;
import br.com.budget.services.BudgetRuleService;
import br.com.budget.services.RevenueService;
import br.com.budget.services.RevenueTypeService;
import br.com.budget.services.SpendingService;
import br.com.budget.services.SpendingTypeService;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Cache do Spring (Caffeine) sobre as leituras agregadas contra Postgres real: o que importa é
 * (1) leitura repetida não vai ao banco, (2) NENHUMA escrita deixa leitura velha (invalidação),
 * (3) chaves não colidem entre donos/serviços. Conta queries via estatísticas do Hibernate.
 * Exige Docker.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class CacheBehaviorIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
            .withDatabaseName("workbox")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("testcontainers-init.sql");

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/workbox"
                .formatted(POSTGRES.getHost(), POSTGRES.getMappedPort(5432)));
        registry.add("spring.datasource.username", () -> "budget_service");
        registry.add("spring.datasource.password", () -> "budget_service");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired private RevenueService revenues;
    @Autowired private SpendingService spendings;
    @Autowired private RevenueTypeService revenueTypes;
    @Autowired private SpendingTypeService spendingTypes;
    @Autowired private BudgetRuleService rules;
    @Autowired private CacheManager cacheManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void clean() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private UUID revenueType(final boolean inTotals) {
        return revenueTypes.save(new RevenueTypeDTO(null, "R" + unique(), inTotals, true)).id();
    }

    private UUID spendingType(final SpendingCategory category) {
        return spendingTypes.save(new SpendingTypeDTO(null, "S" + unique(), category)).id();
    }

    private void revenue(final UUID type, final String owner, final String value, final LocalDate date) {
        revenues.save(new RevenueDTO(null, type, null, new BigDecimal(value), date, date), owner);
    }

    private void spending(final UUID type, final String owner, final String value, final LocalDate date) {
        spendings.save(new SpendingDTO(null, type, null, null, new BigDecimal(value), date, date, true), owner);
    }

    private long queries() {
        return statistics.getQueryExecutionCount();
    }

    @Test
    void cacheManager_isCaffeineWithExpiryAndNamedCaches() {
        assertThat(cacheManager.getCacheNames()).contains("budget-aggregates", "budget-types");
        final var cache = (CaffeineCache) cacheManager.getCache("budget-aggregates");
        final Cache<Object, Object> nativeCache = cache.getNativeCache();
        assertThat(nativeCache.policy().expireAfterWrite()).isPresent();
        assertThat(nativeCache.policy().eviction().orElseThrow().getMaximum()).isPositive();
    }

    @Test
    void repeatedAggregateReads_doNotHitTheDatabaseAgain() {
        final var owner = "cache-" + unique();
        revenue(revenueType(true), owner, "100.00", LocalDate.of(2051, 3, 10));
        cacheManager.getCache("budget-aggregates").clear();

        final var first = revenues.total(3, 2051, null, owner).getTotal();
        final var afterFirst = queries();
        final var second = revenues.total(3, 2051, null, owner).getTotal();
        revenues.totalPorTipo(3, 2051, owner);
        final var afterByType = queries();
        revenues.totalPorTipo(3, 2051, owner);
        rules.cinquentaTrintaVinte(3, 2051, owner);
        final var afterRule = queries();
        rules.cinquentaTrintaVinte(3, 2051, owner);
        rules.resumoMensal(3, 2051, owner);
        final var afterSummary = queries();
        rules.resumoMensal(3, 2051, owner);
        rules.resumoAnual(2051, owner);
        final var afterYearly = queries();
        rules.resumoAnual(2051, owner);
        rules.serieMensal(2051, owner);
        final var afterSeries = queries();
        rules.serieMensal(2051, owner);
        spendings.total(3, 2051, null, owner);
        final var afterSpendingTotal = queries();
        spendings.total(3, 2051, null, owner);
        spendings.totalPorTipo(3, 2051, owner);
        final var afterSpendingByType = queries();
        spendings.totalPorTipo(3, 2051, owner);

        assertThat(second).isEqualByComparingTo(first);
        assertThat(afterByType).as("2ª leitura de total").isGreaterThan(afterFirst);
        assertThat(queries()).as("nenhuma query nas repetições").isEqualTo(afterSpendingByType);
        assertThat(afterRule).isGreaterThan(afterByType);
        assertThat(afterSummary).isGreaterThan(afterRule);
        assertThat(afterYearly).isGreaterThan(afterSummary);
        assertThat(afterSeries).isGreaterThan(afterYearly);
        // já cacheado por dentro de resumoMensal (mesma classe/método/parâmetros → mesma chave)
        assertThat(afterSpendingTotal).isEqualTo(afterSeries);
        assertThat(afterSpendingByType).isGreaterThan(afterSpendingTotal);
    }

    @Test
    void keys_doNotCollideBetweenOwnersOrBetweenRevenueAndSpendingWithTheSameParameters() {
        final var ownerA = "a-" + unique();
        final var ownerB = "b-" + unique();
        final var spendingType = spendingType(SpendingCategory.ESSENTIAL);
        revenue(revenueType(true), ownerA, "1000.00", LocalDate.of(2052, 5, 1));
        revenue(revenueType(true), ownerB, "7.00", LocalDate.of(2052, 5, 1));
        spending(spendingType, ownerA, "300.00", LocalDate.of(2052, 5, 1));

        assertThat(revenues.total(5, 2052, null, ownerA).getTotal()).isEqualByComparingTo("1000.00");
        assertThat(revenues.total(5, 2052, null, ownerB).getTotal()).isEqualByComparingTo("7.00");
        assertThat(spendings.total(5, 2052, null, ownerA).getTotal()).isEqualByComparingTo("300.00");
        // de novo, já do cache — continua cada um com o seu
        assertThat(revenues.total(5, 2052, null, ownerA).getTotal()).isEqualByComparingTo("1000.00");
        assertThat(revenues.total(5, 2052, null, ownerB).getTotal()).isEqualByComparingTo("7.00");
        assertThat(spendings.total(5, 2052, null, ownerA).getTotal()).isEqualByComparingTo("300.00");
    }

    @Test
    void savingARevenue_evictsAggregates_soTheNextReadSeesIt() {
        final var owner = "save-" + unique();
        final var type = revenueType(true);
        revenue(type, owner, "100.00", LocalDate.of(2053, 2, 1));
        assertThat(revenues.total(2, 2053, null, owner).getTotal()).isEqualByComparingTo("100.00");
        assertThat(rules.resumoAnual(2053, owner).totalRevenue()).isEqualByComparingTo("100.00");
        assertThat(rules.serieMensal(2053, owner).get(1).totalRevenue()).isEqualByComparingTo("100.00");

        revenue(type, owner, "50.00", LocalDate.of(2053, 2, 2));

        assertThat(revenues.total(2, 2053, null, owner).getTotal()).isEqualByComparingTo("150.00");
        assertThat(rules.resumoAnual(2053, owner).totalRevenue()).isEqualByComparingTo("150.00");
        assertThat(rules.serieMensal(2053, owner).get(1).totalRevenue()).isEqualByComparingTo("150.00");
    }

    @Test
    void updatingAndDeletingARevenue_evictAggregates() {
        final var owner = "upd-" + unique();
        final var type = revenueType(true);
        final var saved = revenues.save(new RevenueDTO(null, type, null, new BigDecimal("100.00"), LocalDate.of(2054, 4, 1), LocalDate.of(2054, 4, 1)), owner);
        assertThat(revenues.total(4, 2054, null, owner).getTotal()).isEqualByComparingTo("100.00");

        revenues.update(saved.id(), new RevenueDTO(saved.id(), type, null, new BigDecimal("250.00"), LocalDate.of(2054, 4, 1), LocalDate.of(2054, 4, 1)), owner);
        assertThat(revenues.total(4, 2054, null, owner).getTotal()).isEqualByComparingTo("250.00");

        revenues.delete(saved.id(), owner);
        assertThat(revenues.total(4, 2054, null, owner).getTotal()).isEqualByComparingTo("0");
    }

    @Test
    void savingASpending_evictsSpendingAndRuleAggregates() {
        final var owner = "sp-" + unique();
        final var essential = spendingType(SpendingCategory.ESSENTIAL);
        spending(essential, owner, "100.00", LocalDate.of(2055, 6, 1));
        assertThat(spendings.total(6, 2055, null, owner).getTotal()).isEqualByComparingTo("100.00");
        assertThat(rules.cinquentaTrintaVinte(6, 2055, owner).essential().actual()).isEqualByComparingTo("100.00");

        spending(essential, owner, "40.00", LocalDate.of(2055, 6, 2));

        assertThat(spendings.total(6, 2055, null, owner).getTotal()).isEqualByComparingTo("140.00");
        assertThat(rules.cinquentaTrintaVinte(6, 2055, owner).essential().actual()).isEqualByComparingTo("140.00");
        assertThat(rules.serieMensal(2055, owner).get(5).essential()).isEqualByComparingTo("140.00");
    }

    @Test
    void batchWrites_evictToo() {
        final var owner = "batch-" + unique();
        final var type = revenueType(true);
        assertThat(revenues.total(7, 2056, null, owner).getTotal()).isEqualByComparingTo("0");

        revenues.saveAll(java.util.List.of(
                new RevenueDTO(null, type, null, new BigDecimal("10.00"), LocalDate.of(2056, 7, 1), LocalDate.of(2056, 7, 1)),
                new RevenueDTO(null, type, null, new BigDecimal("20.00"), LocalDate.of(2056, 7, 2), LocalDate.of(2056, 7, 2))), owner);

        assertThat(revenues.total(7, 2056, null, owner).getTotal()).isEqualByComparingTo("30.00");
    }

    @Test
    void changingARevenueTypeFlag_evictsTheByTypeTotals() {
        final var owner = "flag-" + unique();
        final var type = revenueType(true);
        revenue(type, owner, "500.00", LocalDate.of(2057, 8, 1));
        assertThat(revenues.totalPorTipo(null, 2057, owner)).extracting(TypeTotalDTO::typeId).contains(type);

        final var current = revenueTypes.findById(type);
        revenueTypes.update(type, new RevenueTypeDTO(type, current.name(), false, current.includeInMonthlyTotals()));

        assertThat(revenues.totalPorTipo(null, 2057, owner)).extracting(TypeTotalDTO::typeId).doesNotContain(type);
        assertThat(rules.resumoAnual(2057, owner).totalRevenue()).isEqualByComparingTo("0");
    }

    @Test
    void typeCatalogs_areCachedAndEvictedOnWrites() {
        final var before = revenueTypes.findAll().size();
        final var statsAfterFirst = queries();
        revenueTypes.findAll();
        assertThat(queries()).as("catálogo repetido vem do cache").isEqualTo(statsAfterFirst);

        revenueType(true);

        assertThat(revenueTypes.findAll()).hasSize(before + 1);

        final var spendingBefore = spendingTypes.findAll().size();
        spendingType(SpendingCategory.PERSONAL);
        assertThat(spendingTypes.findAll()).hasSize(spendingBefore + 1);
    }
}
