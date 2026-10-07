package br.com.budget.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * Chave = classe + método + parâmetros. A chave default do Spring usa só os parâmetros, e
 * {@code RevenueService.total(3, 2026, null, dono)} colidiria com {@code SpendingService.total(...)}.
 * O dono ({@code ownerUsername}) é sempre parâmetro dos métodos em cache, então a chave isola usuários.
 */
public class BudgetKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(final Object target, final Method method, final Object... params) {
        return List.of(method.getDeclaringClass().getSimpleName(), method.getName(), Arrays.asList(params));
    }
}
