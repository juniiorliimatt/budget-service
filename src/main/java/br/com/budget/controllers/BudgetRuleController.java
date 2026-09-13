package br.com.budget.controllers;

import br.com.budget.models.dto.FiftyThirtyTwentyDTO;
import br.com.budget.services.BudgetRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Regras de orçamento derivadas de receitas + despesas — mês/ano sempre obrigatórios. */
@RestController
@RequestMapping("/api/v1/budget-rules")
public class BudgetRuleController {

    private final BudgetRuleService budgetRuleService;

    public BudgetRuleController(final BudgetRuleService budgetRuleService) {
        this.budgetRuleService = budgetRuleService;
    }

    @GetMapping("/fifty-thirty-twenty")
    public ResponseEntity<FiftyThirtyTwentyDTO> fiftyThirtyTwenty(@RequestParam int month, @RequestParam int year,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(budgetRuleService.fiftyThirtyTwenty(month, year, authentication.getName()));
    }
}
