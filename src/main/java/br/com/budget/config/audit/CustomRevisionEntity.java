package br.com.budget.config.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

/**
 * Revisão do Envers com quem fez a mudança — {@link RevisionListenerImpl} preenche
 * {@code username} a partir do SecurityContext, mesma fonte que {@code AuditorAwareImpl}
 * usa pra createdBy/updatedBy. Mesmo padrão do workbox-api (config/audit/CustomRevisionEntity).
 */
@Getter
@Setter
@Entity
@Table(name = "rev_info", schema = "budget")
@RevisionEntity(RevisionListenerImpl.class)
public class CustomRevisionEntity extends DefaultRevisionEntity {

    @Column(nullable = false)
    private String username;
}
