package br.com.budget.revenue.repositories;

import br.com.budget.revenue.entities.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RevenueRepository extends JpaRepository<Revenue, UUID> {
}
