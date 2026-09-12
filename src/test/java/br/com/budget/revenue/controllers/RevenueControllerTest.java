package br.com.budget.revenue.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.budget.revenue.dto.RevenueDTO;
import br.com.budget.revenue.services.RevenueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@ActiveProfiles("test")
@WebMvcTest(RevenueController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class RevenueControllerTest {

    final private static String API_V1_REVENUES = "/api/v1/revenues";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private RevenueService revenueService;

    @Test
    void findAll_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(API_V1_REVENUES)).andExpect(status().isUnauthorized());
    }

    @Test
    void findAll_withAuth_returnsPage() throws Exception {
        var dto = new RevenueDTO(UUID.randomUUID(), "Salary", BigDecimal.valueOf(5000), LocalDate.now());
        Page<RevenueDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(revenueService.findAll(any())).thenReturn(page);

        mockMvc.perform(
            get(API_V1_REVENUES)
                .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER")))
            ).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].name").value("Salary"));
    }

    @Test
    void save_withValidBody_returnsCreated() throws Exception {
        var id = UUID.randomUUID();
        var dto = new RevenueDTO(id, "Freelance", BigDecimal.valueOf(1200), LocalDate.now());
        when(revenueService.save(any())).thenReturn(dto);

        mockMvc.perform(post(API_V1_REVENUES)
                .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(
                                new br.com.budget.revenue.dto.RevenueInsertOrUpdateDTO("Freelance", BigDecimal.valueOf(1200), LocalDate.now()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Freelance"));
    }
}
