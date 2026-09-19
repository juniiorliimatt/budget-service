package br.com.budget.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.budget.exceptions.ResourceInUseException;
import br.com.budget.models.dto.RevenueTypeDTO;
import br.com.budget.models.dto.RevenueTypeRevisionDTO;
import br.com.budget.services.AuditService;
import br.com.budget.services.RevenueTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(RevenueTypeController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class RevenueTypeControllerTest {

    private static final String API_V1_REVENUE_TYPES = "/api/v1/revenue-types";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private RevenueTypeService service;

    @MockitoBean
    private AuditService auditService;

    @Test
    void findAll_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(API_V1_REVENUE_TYPES)).andExpect(status().isUnauthorized());
    }

    @Test
    void findAll_withAuth_returnsList() throws Exception {
        when(service.findAll()).thenReturn(List.of(new RevenueTypeDTO(UUID.randomUUID(), "Salário", true, true)));

        mockMvc.perform(get(API_V1_REVENUE_TYPES)
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Salário"));
    }

    @Test
    void save_withValidBody_returnsCreated() throws Exception {
        final var dto = new RevenueTypeDTO(UUID.randomUUID(), "Freelance", true, true);
        when(service.save(any())).thenReturn(dto);

        mockMvc.perform(post(API_V1_REVENUE_TYPES)
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Freelance"));
    }

    @Test
    void delete_withAuth_returnsNoContent() throws Exception {
        mockMvc.perform(delete(API_V1_REVENUE_TYPES + "/" + UUID.randomUUID())
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete de tipo ainda referenciado por receita responde 409 com mensagem específica (regressão: mensagem genérica)")
    void delete_typeInUse_returnsConflictWithSpecificMessage() throws Exception {
        final var id = UUID.randomUUID();
        doThrow(new ResourceInUseException("Cannot delete revenue type 'Salário': still referenced by 2 revenue(s)"))
                .when(service).delete(id);

        mockMvc.perform(delete(API_V1_REVENUE_TYPES + "/" + id)
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot delete revenue type 'Salário': still referenced by 2 revenue(s)"));
    }

    @Test
    void history_withAuth_returnsRevisions() throws Exception {
        final var id = UUID.randomUUID();
        final var revision = new RevenueTypeRevisionDTO(1, LocalDateTime.now(), "qa.admin@workbox.local", "ADD", id, "Salário", true, true);
        when(auditService.findRevenueTypeHistory(id)).thenReturn(List.of(revision));

        mockMvc.perform(get(API_V1_REVENUE_TYPES + "/" + id + "/history")
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Salário"));
    }
}
