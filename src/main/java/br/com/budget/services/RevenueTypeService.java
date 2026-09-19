package br.com.budget.services;

import br.com.budget.exceptions.DuplicateResourceException;
import br.com.budget.exceptions.ResourceInUseException;
import br.com.budget.exceptions.ResourceNotFoundException;
import br.com.budget.models.dto.RevenueTypeDTO;
import br.com.budget.models.entities.RevenueType;
import br.com.budget.repositories.RevenueRepository;
import br.com.budget.repositories.RevenueTypeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueTypeService {

    private final RevenueTypeRepository repository;
    private final RevenueRepository revenueRepository;
    private final MessageSourceAccessor messages;

    public RevenueTypeService(final RevenueTypeRepository repository, final RevenueRepository revenueRepository,
                               final MessageSourceAccessor messages) {
        this.repository = repository;
        this.revenueRepository = revenueRepository;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<RevenueTypeDTO> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream().map(RevenueTypeDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public RevenueTypeDTO findById(final UUID id) {
        return RevenueTypeDTO.from(findEntityById(id));
    }

    @Transactional
    public RevenueTypeDTO save(final RevenueTypeDTO dto) {
        if (repository.existsByNameIgnoreCase(dto.name())) {
            throw new DuplicateResourceException(messages.getMessage("revenueType.jaExiste", new Object[]{dto.name()}));
        }
        final var saved = repository.save(RevenueType.builder().name(dto.name())
                .includeInTotals(dto.includeInTotals() == null || dto.includeInTotals())
                .includeInMonthlyTotals(dto.includeInMonthlyTotals() == null || dto.includeInMonthlyTotals())
                .build());
        return RevenueTypeDTO.from(saved);
    }

    @Transactional
    public RevenueTypeDTO update(final UUID id, final RevenueTypeDTO dto) {
        final var entity = findEntityById(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(dto.name(), id)) {
            throw new DuplicateResourceException(messages.getMessage("revenueType.jaExiste", new Object[]{dto.name()}));
        }
        entity.setName(dto.name());
        // omitido no PUT preserva o valor atual, nunca reseta pra true silenciosamente.
        if (dto.includeInTotals() != null) {
            entity.setIncludeInTotals(dto.includeInTotals());
        }
        if (dto.includeInMonthlyTotals() != null) {
            entity.setIncludeInMonthlyTotals(dto.includeInMonthlyTotals());
        }
        return RevenueTypeDTO.from(repository.save(entity));
    }

    /**
     * Checagem explícita em vez de deixar a FK estourar {@code DataIntegrityViolationException}
     * — dá pra nomear o tipo e quantos lançamentos ainda o referenciam, em vez da
     * mensagem genérica ("this record is still in use") que serve pra qualquer conflito
     * de integridade referencial do sistema.
     */
    @Transactional
    public void delete(final UUID id) {
        final var entity = findEntityById(id);
        final var usageCount = revenueRepository.countByType_Id(id);
        if (usageCount > 0) {
            throw new ResourceInUseException(messages.getMessage("revenueType.emUso", new Object[]{entity.getName(), usageCount}));
        }
        repository.delete(entity);
    }

    private RevenueType findEntityById(final UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(messages.getMessage("revenueType.naoEncontrado")));
    }
}
