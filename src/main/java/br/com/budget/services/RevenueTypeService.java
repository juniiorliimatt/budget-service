package br.com.budget.services;

import br.com.budget.exceptions.DuplicateResourceException;
import br.com.budget.exceptions.ResourceNotFoundException;
import br.com.budget.models.dto.RevenueTypeDTO;
import br.com.budget.models.entities.RevenueType;
import br.com.budget.repositories.RevenueTypeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueTypeService {

    private static final String NOT_FOUND = "Revenue type not found";

    private final RevenueTypeRepository repository;

    public RevenueTypeService(final RevenueTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<RevenueTypeDTO> findAll() {
        return repository.findAll().stream().map(RevenueTypeDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public RevenueTypeDTO findById(final UUID id) {
        return RevenueTypeDTO.from(findEntityById(id));
    }

    @Transactional
    public RevenueTypeDTO save(final RevenueTypeDTO dto) {
        if (repository.existsByNameIgnoreCase(dto.name())) {
            throw new DuplicateResourceException("Revenue type already exists: " + dto.name());
        }
        final var saved = repository.save(RevenueType.builder().name(dto.name()).build());
        return RevenueTypeDTO.from(saved);
    }

    @Transactional
    public RevenueTypeDTO update(final UUID id, final RevenueTypeDTO dto) {
        final var entity = findEntityById(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(dto.name(), id)) {
            throw new DuplicateResourceException("Revenue type already exists: " + dto.name());
        }
        entity.setName(dto.name());
        return RevenueTypeDTO.from(repository.save(entity));
    }

    @Transactional
    public void delete(final UUID id) {
        final var entity = findEntityById(id);
        repository.delete(entity);
    }

    private RevenueType findEntityById(final UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }
}
