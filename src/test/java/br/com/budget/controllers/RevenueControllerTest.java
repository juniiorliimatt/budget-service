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

import br.com.budget.models.dto.RevenueAnnualBatchRequestDTO;
import br.com.budget.models.dto.RevenueBatchRequestDTO;
import br.com.budget.models.dto.RevenueDTO;
import br.com.budget.models.dto.RevenueRevisionDTO;
import br.com.budget.models.dto.TotalDTO;
import br.com.budget.models.dto.TypeTotalDTO;
import br.com.budget.services.AuditService;
import br.com.budget.services.RevenueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
@WebMvcTest(RevenueController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class RevenueControllerTest {

    private static final String API_V1_REVENUES = "/api/v1/revenues";
    private static final String OWNER = "qa.admin@workbox.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private RevenueService revenueService;

    @MockitoBean
    private AuditService auditService;

    private RequestPostProcessor auth() {
        return opaqueToken()
                .attributes(attrs -> attrs.put("sub", OWNER))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private RevenueDTO dto(String typeName, BigDecimal value) {
        return new RevenueDTO(UUID.randomUUID(), UUID.randomUUID(), typeName, value, LocalDate.now(), LocalDate.now());
    }

    @Test
    void search_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(API_V1_REVENUES)).andExpect(status().isUnauthorized());
    }

    @Test
    void search_withAuth_returnsPage() throws Exception {
        var dto = dto("Salary", BigDecimal.valueOf(5000));
        Page<RevenueDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(revenueService.search(isNull(), isNull(), isNull(), eq(OWNER), any())).thenReturn(page);

        mockMvc.perform(get(API_V1_REVENUES).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].typeName").value("Salary"));
    }

    @Test
    void search_withMonthYearAndType_passesFiltersThrough() throws Exception {
        var typeId = UUID.randomUUID();
        Page<RevenueDTO> page = new PageImpl<>(List.of());
        when(revenueService.search(eq(9), eq(2026), eq(typeId), eq(OWNER), any())).thenReturn(page);

        mockMvc.perform(get(API_V1_REVENUES)
                        .param("month", "9")
                        .param("year", "2026")
                        .param("typeId", typeId.toString())
                        .with(auth()))
                .andExpect(status().isOk());
    }

    @Test
    void total_withAuth_returnsSum() throws Exception {
        when(revenueService.total(9, 2026, null, OWNER)).thenReturn(new TotalDTO(BigDecimal.valueOf(1500)));

        mockMvc.perform(get(API_V1_REVENUES + "/total")
                        .param("month", "9")
                        .param("year", "2026")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1500));
    }

    @Test
    void save_withValidBody_returnsCreated() throws Exception {
        var dto = dto("Freelance", BigDecimal.valueOf(1200));
        when(revenueService.save(any(), eq(OWNER))).thenReturn(dto);

        mockMvc.perform(post(API_V1_REVENUES)
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeName").value("Freelance"));
    }

    @Test
    void save_withZeroValue_returnsCreated() throws Exception {
        var dto = dto("Bônus", BigDecimal.ZERO);
        when(revenueService.save(any(), eq(OWNER))).thenReturn(dto);

        mockMvc.perform(post(API_V1_REVENUES)
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void delete_withAuth_returnsNoContent() throws Exception {
        mockMvc.perform(delete(API_V1_REVENUES + "/" + UUID.randomUUID()).with(auth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void saveAll_withValidBody_returnsCreated() throws Exception {
        var batch = new RevenueBatchRequestDTO(List.of(dto("Salary", BigDecimal.valueOf(5000)), dto("Freelance", BigDecimal.valueOf(1200))));
        when(revenueService.saveAll(any(), eq(OWNER))).thenReturn(batch.revenues());

        mockMvc.perform(post(API_V1_REVENUES + "/batch")
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(batch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void saveAll_withEmptyList_returnsBadRequest() throws Exception {
        var batch = new RevenueBatchRequestDTO(List.of());

        mockMvc.perform(post(API_V1_REVENUES + "/batch")
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(batch)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveAll_withRawArrayInsteadOfEnvelope_returnsBadRequestNotServerError() throws Exception {
        mockMvc.perform(post(API_V1_REVENUES + "/batch")
                        .with(auth())
                        .contentType("application/json")
                        .content("[{\"typeId\":\"" + UUID.randomUUID() + "\",\"value\":100,\"date\":\"2026-09-01\"}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveAnnual_withoutMonths_returnsCreatedWithTwelveEntriesPerItem() throws Exception {
        var typeId = UUID.randomUUID();
        var template = new RevenueDTO(null, typeId, null, BigDecimal.valueOf(5000), LocalDate.of(2026, 1, 5), null);
        var request = new RevenueAnnualBatchRequestDTO(List.of(template), null);
        var generated = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new RevenueDTO(UUID.randomUUID(), typeId, "Salary", BigDecimal.valueOf(5000),
                        LocalDate.of(2026, month, 5), LocalDate.of(2026, month, 5)))
                .toList();
        when(revenueService.saveAnnual(any(), eq(OWNER))).thenReturn(generated);

        mockMvc.perform(post(API_V1_REVENUES + "/batch/annual")
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void saveAnnual_withSelectedMonths_returnsCreatedWithMatchingCount() throws Exception {
        var typeId = UUID.randomUUID();
        var template = new RevenueDTO(null, typeId, null, BigDecimal.valueOf(5000), LocalDate.of(2026, 1, 5), null);
        var request = new RevenueAnnualBatchRequestDTO(List.of(template), Set.of(1, 7));
        var generated = List.of(
                new RevenueDTO(UUID.randomUUID(), typeId, "Salary", BigDecimal.valueOf(5000), LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5)),
                new RevenueDTO(UUID.randomUUID(), typeId, "Salary", BigDecimal.valueOf(5000), LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 5)));
        when(revenueService.saveAnnual(any(), eq(OWNER))).thenReturn(generated);

        mockMvc.perform(post(API_V1_REVENUES + "/batch/annual")
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void saveAnnual_withEmptyRevenuesList_returnsBadRequest() throws Exception {
        var request = new RevenueAnnualBatchRequestDTO(List.of(), null);

        mockMvc.perform(post(API_V1_REVENUES + "/batch/annual")
                        .with(auth())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveAnnual_withInvalidMonth_returnsBadRequest() throws Exception {
        var payload = "{\"revenues\":[{\"typeId\":\"" + UUID.randomUUID() + "\",\"value\":5000,\"date\":\"2026-01-05\"}],\"months\":[0]}";

        mockMvc.perform(post(API_V1_REVENUES + "/batch/annual")
                        .with(auth())
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void totalByType_withAuth_returnsGroupedTotals() throws Exception {
        var typeId = UUID.randomUUID();
        when(revenueService.totalByType(2026, OWNER)).thenReturn(List.of(new TypeTotalDTO(typeId, "Salário", BigDecimal.valueOf(60000))));

        mockMvc.perform(get(API_V1_REVENUES + "/by-type").param("year", "2026").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].typeName").value("Salário"))
                .andExpect(jsonPath("$[0].total").value(60000));
    }

    @Test
    void history_withAuth_returnsRevisions() throws Exception {
        var id = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        var revision = new RevenueRevisionDTO(1, LocalDateTime.now(), OWNER, "ADD", id, typeId, BigDecimal.valueOf(1000), LocalDate.now(), LocalDate.now());
        when(auditService.findRevenueHistory(id, OWNER)).thenReturn(List.of(revision));

        mockMvc.perform(get(API_V1_REVENUES + "/" + id + "/history").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revisionType").value("ADD"))
                .andExpect(jsonPath("$[0].value").value(1000));
    }
}
