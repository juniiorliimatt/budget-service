package br.com.budget.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.budget.models.dto.SpendingTypeDTO;
import br.com.budget.models.enums.SpendingCategory;
import br.com.budget.services.SpendingTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
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
        var dto = new SpendingTypeDTO(UUID.randomUUID(), "Aluguel", SpendingCategory.ESSENTIAL);
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
}
