package davi.spf.supportflow.ticket.controller;

import davi.spf.supportflow.history.dto.TicketHistoryResponseDTO;
import davi.spf.supportflow.history.service.TicketHistoryService;
import davi.spf.supportflow.ticket.dto.AssignTicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketResponseDTO;
import davi.spf.supportflow.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketHistoryService ticketHistoryService;

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@RequestBody @Valid TicketRequestDTO dto,
                                                          Authentication authentication) {

        return ResponseEntity.ok(ticketService.createTicket(dto, authentication));
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponseDTO>> listTickets(Authentication authentication,
                                                               Pageable pageable) {

        return ResponseEntity.ok(ticketService.listTickets(authentication, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.getTicketById(id, authentication));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<TicketResponseDTO> assignTicket(@PathVariable Long id,
                                                          @RequestBody @Valid AssignTicketRequestDTO technicianId,
                                                          Authentication authentication) {

        TicketResponseDTO response = ticketService.assignTicket(technicianId, id, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/claim")
    public ResponseEntity<TicketResponseDTO> claimTicket(@PathVariable Long id, Authentication authentication) {
        TicketResponseDTO response = ticketService.claimTicket(id, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<TicketResponseDTO> resolveTicket(@PathVariable Long id, Authentication authentication) {
        TicketResponseDTO response = ticketService.resolveTicket(id, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<TicketResponseDTO> closeTicket(@PathVariable Long id, Authentication authentication) {
        TicketResponseDTO response = ticketService.closeTicket(id, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TicketResponseDTO> cancelTicket(@PathVariable Long id, Authentication authentication) {
        TicketResponseDTO response = ticketService.cancelTicket(id, authentication);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticketId}/history")
    public ResponseEntity<Page<TicketHistoryResponseDTO>> listHistory(@PathVariable Long ticketId,
                                                                      Authentication authentication,
                                                                      Pageable pageable) {

        Page<TicketHistoryResponseDTO> history = ticketHistoryService.listHistory(
                ticketId,
                authentication,
                pageable
        );

        return ResponseEntity.ok(history);
    }
}
