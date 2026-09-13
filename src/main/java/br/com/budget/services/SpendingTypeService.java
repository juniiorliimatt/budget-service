package br.com.budget.services;

import br.com.budget.exceptions.DuplicateResourceException;
import br.com.budget.exceptions.ResourceNotFoundException;
import br.com.budget.models.dto.SpendingTypeDTO;
import br.com.budget.models.entities.SpendingType;
import br.com.budget.repositories.SpendingTypeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpendingTypeService {

    private static final String NOT_FOUND = "Spending type not found";

    private final SpendingTypeRepository repository;

    public SpendingTypeService(final SpendingTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SpendingTypeDTO> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream().map(SpendingTypeDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public SpendingTypeDTO findById(final UUID id) {
        return SpendingTypeDTO.from(findEntityById(id));
    }

    @Transactional
    public SpendingTypeDTO save(final SpendingTypeDTO dto) {
        if (repository.existsByNameIgnoreCase(dto.name())) {
            throw new DuplicateResourceException("Spending type already exists: " + dto.name());
        }
        final var saved = repository.save(SpendingType.builder().name(dto.name()).category(dto.category()).build());
        return SpendingTypeDTO.from(saved);
    }

    @Transactional
    public SpendingTypeDTO update(final UUID id, final SpendingTypeDTO dto) {
        final var entity = findEntityById(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(dto.name(), id)) {
            throw new DuplicateResourceException("Spending type already exists: " + dto.name());
        }
        entity.setName(dto.name());
        entity.setCategory(dto.category());
        return SpendingTypeDTO.from(repository.save(entity));
    }

    @Transactional
    public void delete(final UUID id) {
        final var entity = findEntityById(id);
        repository.delete(entity);
    }

    private SpendingType findEntityById(final UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }
}
