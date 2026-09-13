package br.com.budget.controllers;

import br.com.budget.models.dto.RevenueTypeDTO;
import br.com.budget.services.RevenueTypeService;
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

/** Catálogo de tipos de receita — CRUD simples pra popular a tela de lançamento. */
@RestController
@RequestMapping("/api/v1/revenue-types")
public class RevenueTypeController {

    private final RevenueTypeService service;

    public RevenueTypeController(final RevenueTypeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<RevenueTypeDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RevenueTypeDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RevenueTypeDTO> save(@RequestBody @Valid RevenueTypeDTO dto, UriComponentsBuilder uriBuilder) {
        final var saved = service.save(dto);
        final URI uri = uriBuilder.path("/api/v1/revenue-types/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(uri).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RevenueTypeDTO> update(@PathVariable UUID id, @RequestBody @Valid RevenueTypeDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
