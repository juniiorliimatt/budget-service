package br.com.budget.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WorkboxTokenIntrospectorTest {

    private static final String URI = "http://workbox-api/api/v1/auth/introspect";

    private MockRestServiceServer server;
    private WorkboxTokenIntrospector introspector;

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        introspector = new WorkboxTokenIntrospector(builder.build(), URI, "budget-service", "segredo");
    }

    @Test
    void introspect_activeToken_mapsRolesAndModulesToAuthorities() {
        server.expect(requestTo(URI)).andRespond(withSuccess(
                "{\"active\":true,\"sub\":\"ana@workbox.local\",\"roles\":[\"ROLE_USER\"],\"modules\":[\"FINANCAS\",\"FORZA\"],\"exp\":1760000000}",
                MediaType.APPLICATION_JSON));

        final var principal = introspector.introspect("abc");

        assertThat(principal.getName()).isEqualTo("ana@workbox.local");
        assertThat(principal.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "MODULE_FINANCAS", "MODULE_FORZA");
    }

    @Test
    void introspect_tokenWithoutModules_hasNoModuleAuthority() {
        server.expect(requestTo(URI)).andRespond(withSuccess(
                "{\"active\":true,\"sub\":\"x\",\"roles\":[\"ROLE_USER\"]}", MediaType.APPLICATION_JSON));

        assertThat(introspector.introspect("t").getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void introspect_inactiveToken_throwsBadOpaqueToken() {
        server.expect(requestTo(URI)).andRespond(withSuccess("{\"active\":false}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> introspector.introspect("t")).isInstanceOf(BadOpaqueTokenException.class);
    }
}
