package br.com.budget.revenue.controllers;

import br.com.budget.revenue.dto.RevenueDTO;
import br.com.budget.revenue.dto.RevenueInsertOrUpdateDTO;
import br.com.budget.revenue.services.RevenueService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/revenues")
public class RevenueController {

    private final RevenueService revenueService;

    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping
    public ResponseEntity<Page<RevenueDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(revenueService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RevenueDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(revenueService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RevenueDTO> save(@RequestBody @Valid RevenueInsertOrUpdateDTO dto, UriComponentsBuilder uriBuilder) {
        var saved = revenueService.save(dto);
        URI uri = uriBuilder.path("/api/revenues/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(uri).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RevenueDTO> update(@PathVariable UUID id, @RequestBody @Valid RevenueInsertOrUpdateDTO dto) {
        return ResponseEntity.ok(revenueService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        revenueService.delete(id);
    }
}
