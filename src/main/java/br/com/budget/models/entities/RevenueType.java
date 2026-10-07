package br.com.budget.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Catálogo de tipos de receita (ex.: Salário, Freelance, Investimento) — cadastrado via
 * CRUD próprio, nunca hardcoded, pra popular a tela de lançamento de receitas.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "revenue_types", schema = "budget")
@EntityListeners(AuditingEntityListener.class)
@Audited
public class RevenueType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "{validacao.nomeObrigatorio}")
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Controla se este tipo entra na soma agrupada por tipo ({@code by-type}) e no total
     * anual "de tudo" ({@code yearly-summary}) — ex.: "Caixinha" é sobra de salário de
     * mês anterior recolocada como receita, já contabilizada dentro do próprio "Salário"
     * quando entrou; incluir de novo aqui duplicaria o valor no anual. Não afeta total
     * mensal nem total de um tipo específico. Default {@code true} quando omitido.
     */
    @NotNull(message = "{validacao.incluirNosTotaisObrigatorio}")
    @Column(name = "include_in_totals", nullable = false)
    private Boolean includeInTotals;

    /**
     * Controla se este tipo entra no total mensal "de tudo" (resumo mensal, regra
     * 50/30/20) — ex.: saldo que sobra de dezembro e é lançado em janeiro pra fechar o
     * ano; contar em janeiro infla o mês com dinheiro que não é receita nova daquele
     * mês. Conta normalmente no anual e no by-type. Default {@code true} quando omitido.
     */
    @NotNull(message = "{validacao.incluirNosTotaisMensaisObrigatorio}")
    @Column(name = "include_in_monthly_totals", nullable = false)
    private Boolean includeInMonthlyTotals;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
