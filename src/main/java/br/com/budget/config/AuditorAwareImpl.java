package br.com.budget.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@code authentication.getName()} funciona tanto pro {@code BearerTokenAuthentication}
 * (opaque token/introspecção, principal atual) quanto pra qualquer outro tipo de
 * {@link Authentication} — nunca fazer cast pra {@code Jwt} aqui: desde a migração pra
 * introspecção remota (ver docs/budget-service-migracao-introspeccao.md na raiz do
 * monorepo) o principal nunca mais é um {@code Jwt}, e um cast direto sempre caía no
 * fallback "system", nunca gravando o usuário real em created_by/updated_by.
 * {@code Optional.empty()} quando não autenticado, mesmo padrão do AuditorAwareImpl do
 * workbox-api — todo endpoint aqui exige autenticação, então esse ramo nunca populariza
 * created_by/updated_by na prática.
 */
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName());
    }
}
