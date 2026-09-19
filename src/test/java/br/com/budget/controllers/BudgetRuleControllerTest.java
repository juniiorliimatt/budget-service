package br.com.budget.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.budget.models.dto.BudgetBucketDTO;
import br.com.budget.models.dto.FiftyThirtyTwentyDTO;
import br.com.budget.models.dto.MonthlySummaryDTO;
import br.com.budget.models.dto.YearlySummaryDTO;
import br.com.budget.services.BudgetRuleService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@ActiveProfiles("test")
@Import(br.com.budget.config.MessageConfig.class)
@WebMvcTest(BudgetRuleController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class BudgetRuleControllerTest {

    private static final String API_V1_BUDGET_RULES = "/api/v1/budget-rules";
    private static final String OWNER = "qa.admin@workbox.local";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetRuleService budgetRuleService;

    private RequestPostProcessor auth() {
        return opaqueToken()
                .attributes(attrs -> attrs.put("sub", OWNER))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void fiftyThirtyTwenty_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(API_V1_BUDGET_RULES + "/fifty-thirty-twenty").param("month", "9").param("year", "2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fiftyThirtyTwenty_withAuth_returnsBreakdown() throws Exception {
        final var dto = new FiftyThirtyTwentyDTO(
                BigDecimal.valueOf(1000),
                BudgetBucketDTO.of(BigDecimal.valueOf(500), BigDecimal.valueOf(600)),
                BudgetBucketDTO.of(BigDecimal.valueOf(300), BigDecimal.valueOf(200)),
                BudgetBucketDTO.of(BigDecimal.valueOf(200), BigDecimal.valueOf(200)));
        when(budgetRuleService.fiftyThirtyTwenty(9, 2026, OWNER)).thenReturn(dto);

        mockMvc.perform(get(API_V1_BUDGET_RULES + "/fifty-thirty-twenty")
                        .param("month", "9")
                        .param("year", "2026")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(1000))
                .andExpect(jsonPath("$.essential.difference").value(100))
                .andExpect(jsonPath("$.personal.difference").value(-100))
                .andExpect(jsonPath("$.savings.difference").value(0));
    }

    @Test
    void monthlySummary_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(API_V1_BUDGET_RULES + "/monthly-summary").param("month", "9").param("year", "2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void monthlySummary_withAuth_returnsSummary() throws Exception {
        final var dto = new MonthlySummaryDTO(
                BigDecimal.valueOf(5000), BigDecimal.valueOf(3200),
                BigDecimal.valueOf(2000), BigDecimal.valueOf(1200), BigDecimal.valueOf(1800));
        when(budgetRuleService.monthlySummary(9, 2026, OWNER)).thenReturn(dto);

        mockMvc.perform(get(API_V1_BUDGET_RULES + "/monthly-summary")
                        .param("month", "9")
                        .param("year", "2026")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(5000))
                .andExpect(jsonPath("$.totalSpending").value(3200))
                .andExpect(jsonPath("$.totalPaid").value(2000))
                .andExpect(jsonPath("$.totalPending").value(1200))
                .andExpect(jsonPath("$.projectedBalance").value(1800));
    }

    @Test
    void yearlySummary_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(API_V1_BUDGET_RULES + "/yearly-summary").param("year", "2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void yearlySummary_withAuth_returnsSummary() throws Exception {
        final var dto = new YearlySummaryDTO(BigDecimal.valueOf(60000), BigDecimal.valueOf(42000), BigDecimal.valueOf(18000));
        when(budgetRuleService.yearlySummary(2026, OWNER)).thenReturn(dto);

        mockMvc.perform(get(API_V1_BUDGET_RULES + "/yearly-summary")
                        .param("year", "2026")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(60000))
                .andExpect(jsonPath("$.totalSpending").value(42000))
                .andExpect(jsonPath("$.balance").value(18000));
    }
}
