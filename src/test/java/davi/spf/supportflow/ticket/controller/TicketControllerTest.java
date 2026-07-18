package davi.spf.supportflow.ticket.controller;

import davi.spf.supportflow.auth.security.CustomJwtAuthenticationConverter;
import davi.spf.supportflow.common.config.ProjectSecurityConfig;
import davi.spf.supportflow.comment.service.TicketCommentService;
import davi.spf.supportflow.history.service.TicketHistoryService;
import davi.spf.supportflow.rating.dto.TicketRatingRequestDTO;
import davi.spf.supportflow.rating.dto.TicketRatingResponseDTO;
import davi.spf.supportflow.rating.service.TicketRatingService;
import davi.spf.supportflow.ticket.dto.AssignTicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketFilterDTO;
import davi.spf.supportflow.ticket.dto.TicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketResponseDTO;
import davi.spf.supportflow.ticket.enums.TicketPriority;
import davi.spf.supportflow.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import({ProjectSecurityConfig.class, CustomJwtAuthenticationConverter.class})
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private TicketHistoryService ticketHistoryService;

    @MockitoBean
    private TicketCommentService ticketCommentService;

    @MockitoBean
    private TicketRatingService ticketRatingService;

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldAllowEmployeeToCreateTicket() throws Exception {
        TicketRequestDTO request = ticketRequest();
        TicketResponseDTO response = ticketResponse(1L, "Printer issue", "OPEN");

        when(ticketService.createTicket(eq(request), any(Authentication.class))).thenReturn(response);

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Printer issue"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(ticketService).createTicket(eq(request), any(Authentication.class));
    }

    @Test
    void shouldRejectUnauthenticatedCreateTicket() throws Exception {
        TicketRequestDTO request = ticketRequest();

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).createTicket(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToAssignTicket() throws Exception {
        AssignTicketRequestDTO request = assignRequest();
        TicketResponseDTO response = ticketResponse(1L, "Printer issue", "IN_PROGRESS");

        when(ticketService.assignTicket(eq(request), eq(1L), any(Authentication.class))).thenReturn(response);

        mockMvc.perform(patch("/tickets/{id}/assign", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(ticketService).assignTicket(eq(request), eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianAssignTicket() throws Exception {
        mockMvc.perform(patch("/tickets/{id}/assign", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest())))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).assignTicket(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldAllowTechnicianToClaimTicket() throws Exception {
        TicketResponseDTO response = ticketResponse(1L, "Printer issue", "IN_PROGRESS");

        when(ticketService.claimTicket(eq(1L), any(Authentication.class))).thenReturn(response);

        mockMvc.perform(patch("/tickets/{id}/claim", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(ticketService).claimTicket(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeClaimTicket() throws Exception {
        mockMvc.perform(patch("/tickets/{id}/claim", 1L))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).claimTicket(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToResolveTicket() throws Exception {
        TicketResponseDTO response = ticketResponse(1L, "Printer issue", "RESOLVED");

        when(ticketService.resolveTicket(eq(1L), any(Authentication.class))).thenReturn(response);

        mockMvc.perform(patch("/tickets/{id}/resolve", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(ticketService).resolveTicket(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldAllowTechnicianToResolveTicket() throws Exception {
        TicketResponseDTO response = ticketResponse(1L, "Printer issue", "RESOLVED");

        when(ticketService.resolveTicket(eq(1L), any(Authentication.class))).thenReturn(response);

        mockMvc.perform(patch("/tickets/{id}/resolve", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(ticketService).resolveTicket(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeResolveTicket() throws Exception {
        mockMvc.perform(patch("/tickets/{id}/resolve", 1L))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).resolveTicket(any(), any());
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldAllowEmployeeToCloseTicket() throws Exception {
        TicketResponseDTO response = ticketResponse(1L, "Printer issue", "CLOSED");

        when(ticketService.closeTicket(eq(1L), any(Authentication.class))).thenReturn(response);

        mockMvc.perform(patch("/tickets/{id}/close", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        verify(ticketService).closeTicket(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianCloseTicket() throws Exception {
        mockMvc.perform(patch("/tickets/{id}/close", 1L))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).closeTicket(any(), any());
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldAllowEmployeeToCreateRating() throws Exception {
        TicketRatingRequestDTO request = ratingRequest();
        TicketRatingResponseDTO response = ratingResponse(10L, 1L);

        when(ticketRatingService.createRating(eq(request), any(Authentication.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/tickets/{id}/rating", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.ticketId").value(1L))
                .andExpect(jsonPath("$.score").value(5));

        verify(ticketRatingService).createRating(eq(request), any(Authentication.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianCreateRating() throws Exception {
        mockMvc.perform(post("/tickets/{id}/rating", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ratingRequest())))
                .andExpect(status().isForbidden());

        verify(ticketRatingService, never()).createRating(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToListTickets() throws Exception {
        Page<TicketResponseDTO> response = new PageImpl<>(List.of(ticketResponse(1L, "Printer issue", "OPEN")));

        when(ticketService.listTickets(any(TicketFilterDTO.class), any(Authentication.class), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Printer issue"));

        verify(ticketService).listTickets(any(TicketFilterDTO.class), any(Authentication.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldAllowTechnicianToListTickets() throws Exception {
        Page<TicketResponseDTO> response = new PageImpl<>(List.of(ticketResponse(1L, "Printer issue", "OPEN")));

        when(ticketService.listTickets(any(TicketFilterDTO.class), any(Authentication.class), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Printer issue"));

        verify(ticketService).listTickets(any(TicketFilterDTO.class), any(Authentication.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldAllowEmployeeToListTickets() throws Exception {
        Page<TicketResponseDTO> response = new PageImpl<>(List.of(ticketResponse(1L, "Printer issue", "OPEN")));

        when(ticketService.listTickets(any(TicketFilterDTO.class), any(Authentication.class), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Printer issue"));

        verify(ticketService).listTickets(any(TicketFilterDTO.class), any(Authentication.class), any(Pageable.class));
    }

    private TicketRequestDTO ticketRequest() {
        return new TicketRequestDTO(
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH,
                10L
        );
    }

    private AssignTicketRequestDTO assignRequest() {
        return new AssignTicketRequestDTO(2L);
    }

    private TicketRatingRequestDTO ratingRequest() {
        return new TicketRatingRequestDTO(5, "Great support");
    }

    private TicketResponseDTO ticketResponse(Long id, String title, String status) {
        return new TicketResponseDTO(
                id,
                title,
                "Ticket description",
                status,
                TicketPriority.HIGH.name(),
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    private TicketRatingResponseDTO ratingResponse(Long id, Long ticketId) {
        return new TicketRatingResponseDTO(
                id,
                ticketId,
                1L,
                "employee",
                5,
                "Great support",
                LocalDateTime.now()
        );
    }
}
