package br.com.budget.repositories;

import br.com.budget.models.entities.Spending;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpendingRepository extends JpaRepository<Spending, UUID>, JpaSpecificationExecutor<Spending> {

    Optional<Spending> findByIdAndOwnerUsername(UUID id, String ownerUsername);

    long countByType_Id(UUID typeId);
}
