package davi.spf.supportflow.dashboard.controller;

import davi.spf.supportflow.dashboard.dto.DashboardSummaryDTO;
import davi.spf.supportflow.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> summary () {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
