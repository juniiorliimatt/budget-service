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

import br.com.budget.models.dto.RevenueDTO;
import br.com.budget.models.dto.RevenueRevisionDTO;
import br.com.budget.models.dto.TotalDTO;
import br.com.budget.services.AuditService;
import br.com.budget.services.RevenueService;
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
