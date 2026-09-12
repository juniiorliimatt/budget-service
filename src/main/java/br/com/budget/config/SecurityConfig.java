package br.com.budget.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Resource server: valida os access tokens emitidos pelo workbox-api via introspecção
 * remota (POST /api/v1/auth/introspect, client credentials) — nunca decodifica o JWT
 * localmente nem conhece jwt.secret/JWT_SECRET. Sem fluxo de login próprio.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${introspection.uri}")
    private String introspectionUri;

    @Value("${introspection.client-id}")
    private String introspectionClientId;

    @Value("${introspection.client-secret}")
    private String introspectionClientSecret;


    @Value("${cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, OpaqueTokenIntrospector introspector) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        httpSecurity.httpBasic(AbstractHttpConfigurer::disable);
        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .anyRequest().authenticated()
        );

        httpSecurity.oauth2ResourceServer(
            oauth2 -> oauth2.opaqueToken(
                opaque -> opaque.introspector(introspector)));
        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector() {
        return new WorkboxTokenIntrospector(RestClient.create(), introspectionUri,
            introspectionClientId, introspectionClientSecret);
    }
}
