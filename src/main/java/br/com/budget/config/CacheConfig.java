package br.com.budget.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Cache do Spring com Caffeine ({@code spring.cache.caffeine.spec}: tamanho máximo + TTL).
 * <p>
 * {@code order = LOWEST_PRECEDENCE - 1} põe o advice de cache <b>por fora</b> do de transação:
 * a invalidação ({@code @CacheEvict}) roda depois do commit, então nenhuma leitura concorrente
 * repopula o cache com dado anterior à escrita; e um acerto de cache nem abre transação.
 * <p>
 * Estratégia de invalidação: toda escrita (save/update/saveAll/salvarAnual/delete) de receita,
 * despesa ou tipo limpa o cache inteiro ({@code allEntries}) — app pessoal, escritas raras perto
 * das leituras, e as agregações dependem de várias tabelas. O cache é local à instância: com mais
 * de uma réplica seria preciso um cache distribuído (Redis) ou TTL curto.
 */
@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheConfig implements CachingConfigurer {

    @Override
    public KeyGenerator keyGenerator() {
        return new BudgetKeyGenerator();
    }
}
