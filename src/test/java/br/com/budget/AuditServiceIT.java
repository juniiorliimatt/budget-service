package br.com.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.budget.exceptions.ResourceNotFoundException;
import br.com.budget.models.dto.RevenueDTO;
import br.com.budget.models.dto.RevenueTypeDTO;
import br.com.budget.models.dto.SpendingDTO;
import br.com.budget.models.dto.SpendingTypeDTO;
import br.com.budget.models.enums.SpendingCategory;
import br.com.budget.services.AuditService;
import br.com.budget.services.RevenueService;
import br.com.budget.services.RevenueTypeService;
import br.com.budget.services.SpendingService;
import br.com.budget.services.SpendingTypeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Histórico de revisões (Hibernate Envers) lido pelo {@link AuditService} contra Postgres real: cada
 * criação/edição gera uma revisão (ADD, MOD) na ordem, com o snapshot da época e o usuário que mexeu;
 * Revenue/Spending só aparecem pro dono (404 pra outro), tipos são catálogo global. Exige Docker.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class AuditServiceIT {

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

    private static final String OWNER = "qa.user@workbox.local";
    private static final String OTHER = "outro@workbox.local";

    @Autowired
    private AuditService auditService;

    @Autowired
    private RevenueService revenueService;

    @Autowired
    private SpendingService spendingService;

    @Autowired
    private RevenueTypeService revenueTypeService;

    @Autowired
    private SpendingTypeService spendingTypeService;

    @BeforeEach
    void authenticate() {
        // O RevisionListenerImpl lê o usuário do SecurityContext (é instanciado pelo Hibernate, sem DI).
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(OWNER, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static String unique(final String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private RevenueTypeDTO newRevenueType() {
        return revenueTypeService.save(new RevenueTypeDTO(null, unique("Salario"), true, true));
    }

    private SpendingTypeDTO newSpendingType() {
        return spendingTypeService.save(new SpendingTypeDTO(null, unique("Mercado"), SpendingCategory.ESSENTIAL));
    }

    @Test
    void revenueHistory_listsAddThenMod_withTheSnapshotOfEachRevisionAndWhoChangedIt() {
        final var type = newRevenueType();
        final var created = revenueService.save(new RevenueDTO(null, type.id(), null, new BigDecimal("1000.00"),
                LocalDate.of(2031, 3, 5), LocalDate.of(2031, 3, 5)), OWNER);
        revenueService.update(created.id(), new RevenueDTO(created.id(), type.id(), null, new BigDecimal("1500.00"),
                LocalDate.of(2031, 3, 6), LocalDate.of(2031, 3, 5)), OWNER);

        final var history = auditService.findRevenueHistory(created.id(), OWNER);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).revisionType()).isEqualTo("ADD");
        assertThat(history.get(0).value()).isEqualByComparingTo("1000.00");
        assertThat(history.get(1).revisionType()).isEqualTo("MOD");
        assertThat(history.get(1).value()).isEqualByComparingTo("1500.00");
        assertThat(history.get(1).date()).isEqualTo(LocalDate.of(2031, 3, 6));
        assertThat(history).allSatisfy(revision -> {
            assertThat(revision.changedBy()).isEqualTo(OWNER);
            assertThat(revision.typeId()).isEqualTo(type.id());
            assertThat(revision.changedAt()).isNotNull();
        });
        assertThat(history.get(0).revision()).isLessThan(history.get(1).revision());
    }

    @Test
    void revenueHistory_ofSomeoneElsesRevenue_isNotFound() {
        final var type = newRevenueType();
        final var created = revenueService.save(new RevenueDTO(null, type.id(), null, new BigDecimal("10.00"),
                LocalDate.of(2031, 3, 5), null), OWNER);

        assertThatThrownBy(() -> auditService.findRevenueHistory(created.id(), OTHER)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void spendingHistory_listsAddThenMod_andHidesItFromOtherUsers() {
        final var type = newSpendingType();
        final var created = spendingService.save(new SpendingDTO(null, type.id(), null, "Compras do mês", new BigDecimal("300.00"),
                LocalDate.of(2031, 4, 2), LocalDate.of(2031, 4, 2), false), OWNER);
        spendingService.update(created.id(), new SpendingDTO(created.id(), type.id(), null, "Compras do mês", new BigDecimal("320.00"),
                LocalDate.of(2031, 4, 2), LocalDate.of(2031, 4, 2), true), OWNER);

        final var history = auditService.findSpendingHistory(created.id(), OWNER);

        assertThat(history).extracting(h -> h.revisionType()).containsExactly("ADD", "MOD");
        assertThat(history.get(0).wasPaid()).isFalse();
        assertThat(history.get(1).wasPaid()).isTrue();
        assertThat(history.get(1).value()).isEqualByComparingTo("320.00");
        assertThat(history.get(1).description()).isEqualTo("Compras do mês");
        assertThat(history).allSatisfy(h -> assertThat(h.changedBy()).isEqualTo(OWNER));
        assertThatThrownBy(() -> auditService.findSpendingHistory(created.id(), OTHER)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void revenueTypeHistory_tracksRenamesAndFlags_andUnknownIdIsNotFound() {
        final var created = newRevenueType();
        final var renamed = unique("Renda");
        revenueTypeService.update(created.id(), new RevenueTypeDTO(created.id(), renamed, false, true));

        final var history = auditService.findRevenueTypeHistory(created.id());

        assertThat(history).extracting(h -> h.revisionType()).containsExactly("ADD", "MOD");
        assertThat(history.get(0).name()).isEqualTo(created.name());
        assertThat(history.get(1).name()).isEqualTo(renamed);
        assertThat(history.get(1).includeInTotals()).isFalse();
        assertThat(history.get(1).includeInMonthlyTotals()).isTrue();
        assertThat(history).allSatisfy(h -> assertThat(h.changedBy()).isEqualTo(OWNER));
        assertThatThrownBy(() -> auditService.findRevenueTypeHistory(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void spendingTypeHistory_tracksCategoryChanges_andUnknownIdIsNotFound() {
        final var created = newSpendingType();
        spendingTypeService.update(created.id(), new SpendingTypeDTO(created.id(), created.name(), SpendingCategory.SAVINGS));

        final var history = auditService.findSpendingTypeHistory(created.id());

        assertThat(history).extracting(h -> h.revisionType()).containsExactly("ADD", "MOD");
        assertThat(history.get(0).category()).isEqualTo(SpendingCategory.ESSENTIAL);
        assertThat(history.get(1).category()).isEqualTo(SpendingCategory.SAVINGS);
        assertThat(history).allSatisfy(h -> assertThat(h.changedBy()).isEqualTo(OWNER));
        assertThatThrownBy(() -> auditService.findSpendingTypeHistory(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }
}
