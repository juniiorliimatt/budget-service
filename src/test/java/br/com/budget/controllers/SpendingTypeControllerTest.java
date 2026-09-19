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
import br.com.budget.models.dto.SpendingTypeDTO;
import br.com.budget.models.dto.SpendingTypeRevisionDTO;
import br.com.budget.models.enums.SpendingCategory;
import br.com.budget.services.AuditService;
import br.com.budget.services.SpendingTypeService;
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
@WebMvcTest(SpendingTypeController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class SpendingTypeControllerTest {

    private static final String API_V1_SPENDING_TYPES = "/api/v1/spending-types";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private SpendingTypeService service;

    @MockitoBean
    private AuditService auditService;

    @Test
    void findAll_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(API_V1_SPENDING_TYPES)).andExpect(status().isUnauthorized());
    }

    @Test
    void findAll_withAuth_returnsList() throws Exception {
        when(service.findAll()).thenReturn(List.of(new SpendingTypeDTO(UUID.randomUUID(), "Mercado", SpendingCategory.ESSENTIAL)));

        mockMvc.perform(get(API_V1_SPENDING_TYPES)
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mercado"));
    }

    @Test
    void save_withValidBody_returnsCreated() throws Exception {
        final var dto = new SpendingTypeDTO(UUID.randomUUID(), "Aluguel", SpendingCategory.ESSENTIAL);
        when(service.save(any())).thenReturn(dto);

        mockMvc.perform(post(API_V1_SPENDING_TYPES)
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Aluguel"));
    }

    @Test
    void delete_withAuth_returnsNoContent() throws Exception {
        mockMvc.perform(delete(API_V1_SPENDING_TYPES + "/" + UUID.randomUUID())
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete de tipo ainda referenciado por despesa responde 409 com mensagem específica (regressão: mensagem genérica)")
    void delete_typeInUse_returnsConflictWithSpecificMessage() throws Exception {
        final var id = UUID.randomUUID();
        doThrow(new ResourceInUseException("Cannot delete spending type 'Enel': still referenced by 3 spending(s)"))
                .when(service).delete(id);

        mockMvc.perform(delete(API_V1_SPENDING_TYPES + "/" + id)
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot delete spending type 'Enel': still referenced by 3 spending(s)"));
    }

    @Test
    void history_withAuth_returnsRevisions() throws Exception {
        final var id = UUID.randomUUID();
        final var revision = new SpendingTypeRevisionDTO(1, LocalDateTime.now(), "qa.admin@workbox.local", "ADD", id, "Mercado", SpendingCategory.ESSENTIAL);
        when(auditService.findSpendingTypeHistory(id)).thenReturn(List.of(revision));

        mockMvc.perform(get(API_V1_SPENDING_TYPES + "/" + id + "/history")
                        .with(opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mercado"));
    }
}
