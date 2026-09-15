package br.com.budget.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.budget.models.dto.SpendingAnnualBatchRequestDTO;
import br.com.budget.models.dto.SpendingBatchRequestDTO;
import br.com.budget.models.dto.SpendingDTO;
import br.com.budget.models.dto.SpendingRevisionDTO;
import br.com.budget.models.dto.TotalDTO;
import br.com.budget.models.dto.TypeTotalDTO;
import br.com.budget.services.AuditService;
import br.com.budget.services.SpendingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@ActiveProfiles("test")
@WebMvcTest(SpendingController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class SpendingControllerTest {

    private static final String API_V1_SPENDINGS = "/api/v1/spendings";
    private static final String OWNER = "qa.admin@workbox.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private SpendingService spendingService;

    @MockitoBean
    private AuditService auditService;

    private RequestPostProcessor auth() {
        return opaqueToken()
                .attributes(attrs -> attrs.put("sub", OWNER))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private SpendingDTO dto(String typeName, BigDecimal value) {
        return new SpendingDTO(UUID.randomUUID(), UUID.randomUUID(), typeName, "Descrição de teste", value, LocalDate.now(), LocalDate.now(), false);
    }

    @Test
    void search_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(API_V1_SPENDINGS)).andExpect(status().isUnauthorized());
    }

    @Test
    void search_withAuth_returnsPage() throws Exception {
        var dto = dto("Mercado", BigDecimal.valueOf(300));
        Page<SpendingDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(spendingService.search(isNull(), isNull(), isNull(), eq(OWNER), any())).thenReturn(page);

        mockMvc.perform(get(API_V1_SPENDINGS).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].typeName").value("Mercado"));
    }

    @Test
    void search_withMonthYearAndType_passesFiltersThrough() throws Exception {
        var typeId = UUID.randomUUID();
        Page<SpendingDTO> page = new PageImpl<>(List.of());
        when(spendingService.search(eq(9), eq(2026), eq(typeId), eq(OWNER), any())).thenReturn(page);

        mockMvc.perform(get(API_V1_SPENDINGS)
                        .param("month", "9")
                        .param("year", "2026")
                        .param("typeId", typeId.toString())
                        .with(auth()))
                .andExpect(status().isOk());
    }

    @Test
    void total_withAuth_returnsSum() throws Exception {
        when(spendingService.total(9, 2026, null, OWNER)).thenReturn(new TotalDTO(BigDecimal.valueOf(450)));

        mockMvc.perform(get(API_V1_SPENDINGS + "/total")
                        .param("month", "9")
                        .param("year", "2026")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(450));
    }

    @Test
    void save_withValidBody_returnsCreated() throws Exception {
        var dto = dto("Aluguel", BigDecimal.valueOf(1500));
        when(spendingService.save(any(), eq(OWNER))).thenReturn(dto);

        mockMvc.perform(post(API_V1_SPENDINGS)
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeName").value("Aluguel"));
    }

    @Test
    void save_withShortDescription_returnsBadRequest() throws Exception {
        var invalid = new SpendingDTO(null, UUID.randomUUID(), null, "ab", BigDecimal.TEN, LocalDate.now(), null, false);

        mockMvc.perform(post(API_V1_SPENDINGS)
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_withAuth_returnsNoContent() throws Exception {
        mockMvc.perform(delete(API_V1_SPENDINGS + "/" + UUID.randomUUID()).with(auth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void saveAll_withValidBody_returnsCreated() throws Exception {
        var batch = new SpendingBatchRequestDTO(List.of(dto("Mercado", BigDecimal.valueOf(300)), dto("Aluguel", BigDecimal.valueOf(1500))));
        when(spendingService.saveAll(any(), eq(OWNER))).thenReturn(batch.spendings());

        mockMvc.perform(post(API_V1_SPENDINGS + "/batch")
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(batch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void saveAll_withEmptyList_returnsBadRequest() throws Exception {
        var batch = new SpendingBatchRequestDTO(List.of());

        mockMvc.perform(post(API_V1_SPENDINGS + "/batch")
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(batch)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveAnnual_withValidBody_returnsCreatedWithTwelveEntries() throws Exception {
        var typeId = UUID.randomUUID();
        var template = new SpendingDTO(null, typeId, null, "Conta de luz", BigDecimal.valueOf(150), LocalDate.of(2026, 1, 10), null, false);
        var request = new SpendingAnnualBatchRequestDTO(template, null);
        var generated = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new SpendingDTO(UUID.randomUUID(), typeId, "Luz", "Conta de luz", BigDecimal.valueOf(150),
                        LocalDate.of(2026, month, 10), LocalDate.of(2026, month, 10), false))
                .toList();
        when(spendingService.saveAnnual(any(), eq(OWNER))).thenReturn(generated);

        mockMvc.perform(post(API_V1_SPENDINGS + "/batch/annual")
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void saveAnnual_withoutDate_returnsBadRequest() throws Exception {
        var payload = "{\"spending\":{\"typeId\":\"" + UUID.randomUUID() + "\",\"value\":150,\"wasPaid\":false}}";

        mockMvc.perform(post(API_V1_SPENDINGS + "/batch/annual")
                        .with(auth())
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void totalByType_withAuth_returnsGroupedTotals() throws Exception {
        var typeId = UUID.randomUUID();
        when(spendingService.totalByType(2026, OWNER)).thenReturn(List.of(new TypeTotalDTO(typeId, "Condomínio", BigDecimal.valueOf(9600))));

        mockMvc.perform(get(API_V1_SPENDINGS + "/by-type").param("year", "2026").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].typeName").value("Condomínio"))
                .andExpect(jsonPath("$[0].total").value(9600));
    }

    @Test
    void history_withAuth_returnsRevisions() throws Exception {
        var id = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        var revision = new SpendingRevisionDTO(1, LocalDateTime.now(), OWNER, "ADD", id, typeId, "Descrição de teste",
                BigDecimal.valueOf(300), LocalDate.now(), LocalDate.now(), false);
        when(auditService.findSpendingHistory(id, OWNER)).thenReturn(List.of(revision));

        mockMvc.perform(get(API_V1_SPENDINGS + "/" + id + "/history").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revisionType").value("ADD"))
                .andExpect(jsonPath("$[0].value").value(300));
    }
}
