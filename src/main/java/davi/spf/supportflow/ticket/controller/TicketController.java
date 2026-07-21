package davi.spf.supportflow.ticket.controller;

import davi.spf.supportflow.comment.dto.TicketCommentRequestDTO;
import davi.spf.supportflow.comment.dto.TicketCommentResponseDTO;
import davi.spf.supportflow.comment.service.TicketCommentService;
import davi.spf.supportflow.history.dto.TicketHistoryResponseDTO;
import davi.spf.supportflow.history.service.TicketHistoryService;
import davi.spf.supportflow.rating.dto.TicketRatingRequestDTO;
import davi.spf.supportflow.rating.dto.TicketRatingResponseDTO;
import davi.spf.supportflow.rating.service.TicketRatingService;
import davi.spf.supportflow.ticket.dto.AssignTicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketFilterDTO;
import davi.spf.supportflow.ticket.dto.TicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketResponseDTO;
import davi.spf.supportflow.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tickets")
@Tag(name = "4. Tickets", description = "Gestão de chamados, comentários, histórico e avaliações")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;
    private final TicketHistoryService ticketHistoryService;
    private final TicketCommentService ticketCommentService;
    private final TicketRatingService ticketRatingService;

    @PostMapping
    @Operation(summary = "Cria um ticket")
    public ResponseEntity<TicketResponseDTO> createTicket(@RequestBody @Valid TicketRequestDTO dto,
                                                          Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(dto, authentication));
    }

    @GetMapping
    @Operation(summary = "Lista tickets com filtros opcionais")
    public ResponseEntity<Page<TicketResponseDTO>> listTickets(@ModelAttribute TicketFilterDTO filterDTO,
                                                               Authentication authentication,
                                                               Pageable pageable) {

        return ResponseEntity.ok(ticketService.listTickets(filterDTO, authentication, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca ticket por id")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.getTicketById(id, authentication));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Atribui ticket a um técnico")
    public ResponseEntity<TicketResponseDTO> assignTicket(@PathVariable Long id,
                                                          @RequestBody @Valid AssignTicketRequestDTO technicianId,
                                                          Authentication authentication) {

        TicketResponseDTO response = ticketService.assignTicket(technicianId, id, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/claim")
    @Operation(summary = "Assume um ticket sem responsável")
    public ResponseEntity<TicketResponseDTO> claimTicket(@PathVariable Long id, Authentication authentication) {
        TicketResponseDTO response = ticketService.claimTicket(id, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolve um ticket")
    public ResponseEntity<TicketResponseDTO> resolveTicket(@PathVariable Long id, Authentication authentication) {
        TicketResponseDTO response = ticketService.resolveTicket(id, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Fecha um ticket resolvido")
    public ResponseEntity<TicketResponseDTO> closeTicket(@PathVariable Long id, Authentication authentication) {
        TicketResponseDTO response = ticketService.closeTicket(id, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancela um ticket")
    public ResponseEntity<TicketResponseDTO> cancelTicket(@PathVariable Long id, Authentication authentication) {
        TicketResponseDTO response = ticketService.cancelTicket(id, authentication);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticketId}/history")
    @Operation(summary = "Lista histórico do ticket")
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

    @PostMapping("/{id}/comments")
    @Operation(summary = "Adiciona comentário ao ticket")
    public ResponseEntity<TicketCommentResponseDTO> makeComment(@PathVariable Long id,
                                                                @RequestBody @Valid TicketCommentRequestDTO dto,
                                                                Authentication authentication) {

        TicketCommentResponseDTO response = ticketCommentService.makeComment(id, dto.comment(), authentication);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "Lista comentários do ticket")
    public ResponseEntity<Page<TicketCommentResponseDTO>> listComments(@PathVariable long id,
                                                                       Authentication authentication,
                                                                       Pageable pageable) {

        Page<TicketCommentResponseDTO> response = ticketCommentService.listComments(id, authentication, pageable);

        return ResponseEntity.ok(response);

    }

    @PostMapping("/{ticketId}/rating")
    @Operation(summary = "Cria avaliação para ticket fechado")
    public ResponseEntity<TicketRatingResponseDTO> createRating(@PathVariable Long ticketId,
                                                                @RequestBody @Valid TicketRatingRequestDTO dto,
                                                                Authentication authentication) {

        TicketRatingResponseDTO response = ticketRatingService.createRating(dto, authentication, ticketId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ticketId}/rating")
    @Operation(summary = "Consulta avaliação do ticket")
    public ResponseEntity<TicketRatingResponseDTO> getRatingByTicket(@PathVariable Long ticketId,
                                                                     Authentication authentication) {

        return ResponseEntity.ok(ticketRatingService.getRatingByTicket(ticketId, authentication));
    }
}
