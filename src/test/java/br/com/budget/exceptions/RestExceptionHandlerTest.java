package br.com.budget.exceptions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.budget.config.MessageConfig;
import br.com.budget.controllers.RevenueTypeController;
import br.com.budget.services.AuditService;
import br.com.budget.services.RevenueTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Erros de roteamento HTTP são do client (404/405/415) — nunca podem cair no catch-all e virar 500. */
@ActiveProfiles("test")
@Import(MessageConfig.class)
@WebMvcTest(RevenueTypeController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class RestExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevenueTypeService service;

    @MockitoBean
    private AuditService auditService;

    private static RequestPostProcessor user() {
        return opaqueToken().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("rota inexistente responde 404 em problem+json, não 500")
    void rotaInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/v1/nao-existe").with(user()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("método HTTP não suportado responde 405 com o cabeçalho Allow")
    void metodoNaoSuportado_retorna405() throws Exception {
        mockMvc.perform(patch("/api/v1/revenue-types").with(user()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.detail").value("Método HTTP não suportado nesta rota"));
    }

    @Test
    @DisplayName("Content-Type não suportado responde 415")
    void tipoDeConteudoNaoSuportado_retorna415() throws Exception {
        mockMvc.perform(post("/api/v1/revenue-types").with(user()).contentType(MediaType.TEXT_PLAIN).content("oi"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("Tipo de conteúdo não suportado"));
    }
}
