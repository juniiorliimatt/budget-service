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

    @NotBlank(message = "Required field name")
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Controla se este tipo entra na soma agrupada por tipo (endpoints {@code by-type})
     * — ex.: "Caixinha" é sobra de salário de mês anterior recolocada como receita, já
     * contabilizada dentro do próprio "Salário" quando entrou; incluir de novo aqui
     * duplicaria o valor na tela de metas. Não afeta o total geral (mês/ano), só o
     * agrupamento por tipo. Default {@code true} quando omitido no insert/update.
     */
    @NotNull(message = "Required field includeInTotals")
    @Column(name = "include_in_totals", nullable = false)
    private Boolean includeInTotals;

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
