package br.com.budget.controllers;

import br.com.budget.models.dto.SpendingDTO;
import br.com.budget.models.dto.TotalDTO;
import br.com.budget.services.SpendingService;
import jakarta.validation.Valid;
import java.net.URI;
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
                                                    @PageableDefault(sort = {"date", "referenceDate"}, direction = Sort.Direction.DESC)
                                                    Pageable pageable, Authentication authentication) {
    return ResponseEntity.ok(spendingService.search(month, year, typeId, authentication.getName(), pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<SpendingDTO> findById(@PathVariable UUID id, Authentication authentication) {
    return ResponseEntity.ok(spendingService.findById(id, authentication.getName()));
  }

  /** Mesmos filtros de {@link #search}, soma o {@code value} em vez de paginar. */
  @GetMapping("/total")
  public ResponseEntity<TotalDTO> total(@RequestParam(required = false) Integer month,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) UUID typeId,
                                         Authentication authentication) {
    return ResponseEntity.ok(spendingService.total(month, year, typeId, authentication.getName()));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<SpendingDTO> save(@RequestBody @Valid SpendingDTO dto, UriComponentsBuilder uriBuilder,
                                           Authentication authentication) {
    final var saved = spendingService.save(dto, authentication.getName());
    final URI uri = uriBuilder.path("/api/v1/spendings/{id}").buildAndExpand(saved.id()).toUri();
    return ResponseEntity.created(uri).body(saved);
  }

  @PutMapping("/{id}")
  public ResponseEntity<SpendingDTO> update(@PathVariable UUID id, @RequestBody @Valid SpendingDTO dto,
                                             Authentication authentication) {
    return ResponseEntity.ok(spendingService.update(id, dto, authentication.getName()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Authentication authentication) {
    spendingService.delete(id, authentication.getName());
  }
}
