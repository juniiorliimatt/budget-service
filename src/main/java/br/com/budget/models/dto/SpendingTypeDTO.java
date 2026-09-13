package br.com.budget.models.dto;

import br.com.budget.models.entities.SpendingType;
import br.com.budget.models.enums.SpendingCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SpendingTypeDTO(UUID id, @NotBlank String name, @NotNull SpendingCategory category) {
    public static SpendingTypeDTO from(SpendingType entity) {
        return new SpendingTypeDTO(entity.getId(), entity.getName(), entity.getCategory());
    }
}
