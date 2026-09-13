package br.com.budget.controllers;

import br.com.budget.models.dto.SpendingDTO;
import br.com.budget.models.dto.TotalDTO;
import br.com.budget.services.SpendingService;
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
 * Despesas -> Saídas
 * @author Junior Lima - oojuniin@outlook.com
 * @since 12/09/2026
 */

@RestController
@RequestMapping("/api/v1/spendings")
public class SpendingController {

  private final SpendingService spendingService;

  public SpendingController(SpendingService spendingService) {
    this.spendingService = spendingService;
  }

  /** {@code month}+{@code year} e {@code typeId} são opcionais e combináveis. */
  @GetMapping
  public ResponseEntity<Page<SpendingDTO>> search(@RequestParam(required = false) Integer month,
                                                    @RequestParam(required = false) Integer year,
                                                    @RequestParam(required = false) UUID typeId,
                                                    Pageable pageable) {
    return ResponseEntity.ok(spendingService.search(month, year, typeId, pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<SpendingDTO> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(spendingService.findById(id));
  }

  /** Mesmos filtros de {@link #search}, soma o {@code value} em vez de paginar. */
  @GetMapping("/total")
  public ResponseEntity<TotalDTO> total(@RequestParam(required = false) Integer month,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) UUID typeId) {
    return ResponseEntity.ok(spendingService.total(month, year, typeId));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<SpendingDTO> save(@RequestBody @Valid SpendingDTO dto, UriComponentsBuilder uriBuilder) {
    final var saved = spendingService.save(dto);
    final URI uri = uriBuilder.path("/api/v1/spendings/{id}").buildAndExpand(saved.id()).toUri();
    return ResponseEntity.created(uri).body(saved);
  }

  @PutMapping("/{id}")
  public ResponseEntity<SpendingDTO> update(@PathVariable UUID id, @RequestBody @Valid SpendingDTO dto) {
    return ResponseEntity.ok(spendingService.update(id, dto));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    spendingService.delete(id);
  }
}
