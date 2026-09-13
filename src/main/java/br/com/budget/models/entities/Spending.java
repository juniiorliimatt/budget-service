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
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
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

@Entity
@Table(name = "spending", schema = "budget")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Spending implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Required field type")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "spending_type_id", nullable = false)
  private SpendingType type;

  @Size(min = 5, max = 250)
  private String description;

  @Column(nullable = false)
  @NotNull(message = "Required field value")
  private BigDecimal value;

  @Column(nullable = false)
  @NotNull(message = "Required field date")
  private LocalDate date;

  @NotNull(message = "Required field wasPaid")
  private Boolean wasPaid;

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
