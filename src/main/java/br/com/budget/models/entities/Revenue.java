package br.com.budget.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Receitas
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "revenues", schema = "budget")
@EntityListeners(AuditingEntityListener.class)
public class Revenue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Required field type")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revenue_type_id", nullable = false)
    private RevenueType type;

    @NotNull(message = "Required field value")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @NotNull(message = "Required field date")
    @Column(nullable = false)
    private LocalDate date;

    /**
     * Competência: mês/ano orçamentário a que este lançamento pertence — usada em todo
     * filtro/total/regra por mês/ano, não {@code date}. Pode divergir de {@code date}
     * (ex.: salário recebido dia 30 que custeia as contas do mês seguinte deve ter
     * {@code referenceDate} no mês seguinte). Default = {@code date} quando omitida.
     */
    @NotNull(message = "Required field referenceDate")
    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    /** Username (subject da introspecção) do dono do lançamento — nunca vem do client. */
    @NotBlank(message = "Required field ownerUsername")
    @Column(name = "owner_username", nullable = false, updatable = false, length = 255)
    private String ownerUsername;

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
