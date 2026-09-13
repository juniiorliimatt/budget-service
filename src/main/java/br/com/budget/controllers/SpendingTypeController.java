package br.com.budget.controllers;

import br.com.budget.models.dto.SpendingTypeDTO;
import br.com.budget.models.dto.SpendingTypeRevisionDTO;
import br.com.budget.services.AuditService;
import br.com.budget.services.SpendingTypeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** Catálogo de tipos de despesa — CRUD simples pra popular a tela de lançamento. */
@RestController
@RequestMapping("/api/v1/spending-types")
public class SpendingTypeController {

    private final SpendingTypeService service;
    private final AuditService auditService;

    public SpendingTypeController(final SpendingTypeService service, final AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<SpendingTypeDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpendingTypeDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /** Histórico de revisões (Hibernate Envers) — catálogo global, qualquer usuário autenticado enxerga. */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<SpendingTypeRevisionDTO>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.findSpendingTypeHistory(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<SpendingTypeDTO> save(@RequestBody @Valid SpendingTypeDTO dto, UriComponentsBuilder uriBuilder) {
        final var saved = service.save(dto);
        final URI uri = uriBuilder.path("/api/v1/spending-types/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(uri).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpendingTypeDTO> update(@PathVariable UUID id, @RequestBody @Valid SpendingTypeDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
