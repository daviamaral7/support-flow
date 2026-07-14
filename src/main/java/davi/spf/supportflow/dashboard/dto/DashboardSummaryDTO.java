package davi.spf.supportflow.dashboard.dto;

public record DashboardSummaryDTO(
        long totalTickets,
        long openTickets,
        long inProgressTickets,
        long resolvedTickets,
        long closedTickets,
        long cancelledTickets,
        long criticalTickets,
        long unassignedTickets
) {
}
