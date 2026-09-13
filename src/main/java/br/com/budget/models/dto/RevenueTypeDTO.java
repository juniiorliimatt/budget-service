package br.com.budget.models.dto;

import br.com.budget.models.entities.RevenueType;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record RevenueTypeDTO(UUID id, @NotBlank String name) {
    public static RevenueTypeDTO from(RevenueType entity) {
        return new RevenueTypeDTO(entity.getId(), entity.getName());
    }
}
