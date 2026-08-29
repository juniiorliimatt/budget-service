package br.com.budget.revenue.services;

import br.com.budget.exceptions.ResourceNotFoundException;
import br.com.budget.revenue.dto.RevenueDTO;
import br.com.budget.revenue.dto.RevenueInsertOrUpdateDTO;
import br.com.budget.revenue.entities.Revenue;
import br.com.budget.revenue.repositories.RevenueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RevenueService {

    private final RevenueRepository revenueRepository;

    public RevenueService(RevenueRepository revenueRepository) {
        this.revenueRepository = revenueRepository;
    }

    @Transactional(readOnly = true)
    public Page<RevenueDTO> findAll(Pageable pageable) {
        return revenueRepository.findAll(pageable).map(RevenueDTO::from);
    }

    @Transactional(readOnly = true)
    public RevenueDTO findById(UUID id) {
        return RevenueDTO.from(findEntityById(id));
    }

    @Transactional
    public RevenueDTO save(RevenueInsertOrUpdateDTO dto) {
        var revenue = Revenue.builder()
                .name(dto.name())
                .value(dto.value())
                .date(dto.date())
                .build();
        return RevenueDTO.from(revenueRepository.save(revenue));
    }

    @Transactional
    public RevenueDTO update(UUID id, RevenueInsertOrUpdateDTO dto) {
        var revenue = findEntityById(id);
        revenue.setName(dto.name());
        revenue.setValue(dto.value());
        revenue.setDate(dto.date());
        return RevenueDTO.from(revenueRepository.save(revenue));
    }

    @Transactional
    public void delete(UUID id) {
        var revenue = findEntityById(id);
        revenueRepository.delete(revenue);
    }

    private Revenue findEntityById(UUID id) {
        return revenueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revenue not found: " + id));
    }
}
