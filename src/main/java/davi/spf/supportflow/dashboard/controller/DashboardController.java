package davi.spf.supportflow.dashboard.controller;

import davi.spf.supportflow.dashboard.dto.DashboardSummaryDTO;
import davi.spf.supportflow.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/dashboard")
@Tag(name = "5. Dashboard", description = "Indicadores resumidos dos chamados")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Retorna resumo dos tickets")
    public ResponseEntity<DashboardSummaryDTO> summary () {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
