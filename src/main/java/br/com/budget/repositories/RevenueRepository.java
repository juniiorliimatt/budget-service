package br.com.budget.repositories;

import br.com.budget.models.entities.Revenue;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RevenueRepository extends JpaRepository<Revenue, UUID>, JpaSpecificationExecutor<Revenue> {

    Optional<Revenue> findByIdAndOwnerUsername(UUID id, String ownerUsername);
}
