package br.com.budget.controllers;

import br.com.budget.models.dto.RevenueDTO;
import br.com.budget.models.dto.TotalDTO;
import br.com.budget.services.RevenueService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Receitas -> Entradas
 * @author Junior Lima - oojuniin@outlook.com
 * @since 12/09/2026
 */

@RestController
@RequestMapping("/api/v1/revenues")
public class RevenueController {

    private final RevenueService revenueService;

    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    /** {@code month}+{@code year} e {@code typeId} são opcionais e combináveis. */
    @GetMapping
    public ResponseEntity<Page<RevenueDTO>> search(@RequestParam(required = false) Integer month,
                                                     @RequestParam(required = false) Integer year,
                                                     @RequestParam(required = false) UUID typeId,
                                                     Pageable pageable) {
        return ResponseEntity.ok(revenueService.search(month, year, typeId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RevenueDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(revenueService.findById(id));
    }

    /** Mesmos filtros de {@link #search}, soma o {@code value} em vez de paginar. */
    @GetMapping("/total")
    public ResponseEntity<TotalDTO> total(@RequestParam(required = false) Integer month,
                                           @RequestParam(required = false) Integer year,
                                           @RequestParam(required = false) UUID typeId) {
        return ResponseEntity.ok(revenueService.total(month, year, typeId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RevenueDTO> save(@RequestBody @Valid RevenueDTO dto, UriComponentsBuilder uriBuilder) {
        final var saved = revenueService.save(dto);
        final URI uri = uriBuilder.path("/api/v1/revenues/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(uri).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RevenueDTO> update(@PathVariable UUID id, @RequestBody @Valid RevenueDTO dto) {
        return ResponseEntity.ok(revenueService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        revenueService.delete(id);
    }
}
