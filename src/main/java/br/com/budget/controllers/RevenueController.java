package br.com.budget.controllers;

import br.com.budget.models.dto.RevenueAnnualBatchRequestDTO;
import br.com.budget.models.dto.RevenueBatchRequestDTO;
import br.com.budget.models.dto.RevenueDTO;
import br.com.budget.models.dto.RevenueRevisionDTO;
import br.com.budget.models.dto.TotalDTO;
import br.com.budget.models.dto.TypeTotalDTO;
import br.com.budget.services.AuditService;
import br.com.budget.services.RevenueService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final AuditService auditService;

    public RevenueController(final RevenueService revenueService, final AuditService auditService) {
        this.revenueService = revenueService;
        this.auditService = auditService;
    }

    /** {@code month}+{@code year} e {@code typeId} são opcionais e combináveis. */
    @GetMapping
    public ResponseEntity<Page<RevenueDTO>> search(@RequestParam(required = false) final Integer month,
                                                     @RequestParam(required = false) final Integer year,
                                                     @RequestParam(required = false) final UUID typeId,
                                                     @PageableDefault(sort = {"date", "referenceDate"}, direction = Sort.Direction.DESC) final Pageable pageable, final Authentication authentication) {
        return ResponseEntity.ok(revenueService.buscar(month, year, typeId, authentication.getName(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RevenueDTO> findById(@PathVariable final UUID id, final Authentication authentication) {
        return ResponseEntity.ok(revenueService.findById(id, authentication.getName()));
    }

    /** Histórico de revisões (Hibernate Envers) — só do dono do lançamento. */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<RevenueRevisionDTO>> history(@PathVariable final UUID id, final Authentication authentication) {
        return ResponseEntity.ok(auditService.findRevenueHistory(id, authentication.getName()));
    }

    /** Mesmos filtros de {@link #search}, soma o {@code value} em vez de paginar. */
    @GetMapping("/total")
    public ResponseEntity<TotalDTO> total(@RequestParam(required = false) final Integer month,
                                           @RequestParam(required = false) final Integer year,
                                           @RequestParam(required = false) final UUID typeId,
                                           final Authentication authentication) {
        return ResponseEntity.ok(revenueService.total(month, year, typeId, authentication.getName()));
    }

    /** Soma agrupada por tipo no ano inteiro (ex.: total de "Salário" em 2026) — tela de metas. */
    @GetMapping("/by-type")
    public ResponseEntity<List<TypeTotalDTO>> totalByType(@RequestParam final int year, final Authentication authentication) {
        return ResponseEntity.ok(revenueService.totalPorTipo(year, authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RevenueDTO> save(@RequestBody @Valid final RevenueDTO dto, final UriComponentsBuilder uriBuilder,
                                            final Authentication authentication) {
        final var saved = revenueService.save(dto, authentication.getName());
        final URI uri = uriBuilder.path("/api/v1/revenues/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(uri).body(saved);
    }

    /** Insere várias receitas de uma vez, numa única transação (tudo ou nada). */
    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<List<RevenueDTO>> saveAll(@RequestBody @Valid final RevenueBatchRequestDTO batch,
                                                      final Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(revenueService.saveAll(batch.revenues(), authentication.getName()));
    }

    /** Replica uma receita recorrente por todos os meses do ano, numa única transação. */
    @PostMapping("/batch/annual")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<List<RevenueDTO>> saveAnnual(@RequestBody @Valid final RevenueAnnualBatchRequestDTO request,
                                                         final Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(revenueService.salvarAnual(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RevenueDTO> update(@PathVariable final UUID id, @RequestBody @Valid final RevenueDTO dto,
                                              final Authentication authentication) {
        return ResponseEntity.ok(revenueService.update(id, dto, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final UUID id, final Authentication authentication) {
        revenueService.delete(id, authentication.getName());
    }
}
