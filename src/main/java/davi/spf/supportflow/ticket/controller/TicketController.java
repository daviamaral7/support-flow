package davi.spf.supportflow.ticket.controller;

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

    @GetMapping("{id}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.getTicketById(id, authentication));
    }
}
