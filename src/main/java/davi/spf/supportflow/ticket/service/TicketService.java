package davi.spf.supportflow.ticket.service;

import davi.spf.supportflow.category.entity.Category;
import davi.spf.supportflow.category.repository.CategoryRepository;
import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.ticket.dto.AssignTicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketResponseDTO;
import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.ticket.enums.TicketStatus;
import davi.spf.supportflow.ticket.mapper.TicketMapper;
import davi.spf.supportflow.ticket.repository.TicketRepository;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.enums.UserStatus;
import davi.spf.supportflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper mapper;


    public TicketResponseDTO createTicket(TicketRequestDTO dto, Authentication authentication) {
        User createdBy = getAuthenticatedUser(authentication);

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.isActive()) {
            throw new BusinessRuleException("Category is inactive");
        }

        Ticket ticket = mapper.toEntity(dto);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(createdBy);
        ticket.setAssignedTo(null);
        ticket.setCategory(category);
        ticket.setClosedAt(null);

        Ticket savedTicket = ticketRepository.save(ticket);

        return mapper.toResponse(savedTicket);
    }

    public Page<TicketResponseDTO> listTickets(Authentication authentication, Pageable pageable) {
        User authenticatedUser = getAuthenticatedUser(authentication);

        UserRole role = authenticatedUser.getRole();

        Page<Ticket> tickets = switch (role) {
            case ADMIN, TECHNICIAN -> ticketRepository.findAll(pageable);
            case EMPLOYEE -> ticketRepository.findByCreatedBy(authenticatedUser, pageable);
        };

        return tickets.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO getTicketById(Long id, Authentication authentication) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User authenticatedUser = getAuthenticatedUser(authentication);

        validateTicketAccess(ticket, authenticatedUser, authentication);

        return mapper.toResponse(ticket);
    }

    public TicketResponseDTO assignTicket(AssignTicketRequestDTO dto, Long ticketId) {
        User technician = userRepository.findById(dto.technicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found"));

        Ticket ticket = findTicketByIdOrThrow(ticketId);

        validateTechnicianCanReceiveTicket(technician);

        if (technician.equals(ticket.getAssignedTo())) {
            throw new BusinessRuleException("Technician already assigned to this ticket");
        }

        if (isFinished(ticket)) {
            throw new BusinessRuleException("Ticket cannot be assigned");
        }

        ticket.assignTo(technician);
        return mapper.toResponse(ticket);
    }

    public TicketResponseDTO claimTicket(Long id, Authentication authentication) {
        User technician = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated technician not found"));

        validateTechnicianCanReceiveTicket(technician);

        Ticket ticket = findTicketByIdOrThrow(id);

        if (ticket.getAssignedTo() != null) {
            throw new BusinessRuleException("Ticket already claimed");
        }

        if (isFinished(ticket)) {
            throw new BusinessRuleException("Ticket cannot be claimed");
        }

        ticket.assignTo(technician);
        return mapper.toResponse(ticket);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private Ticket findTicketByIdOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }

    private void validateTicketAccess(Ticket ticket, User authenticatedUser, Authentication authentication) {
        boolean isAdmin = hasRole(authentication, "ADMIN");
        boolean isTechnician = hasRole(authentication, "TECHNICIAN");

        if (isAdmin || isTechnician) {
            return;
        }

        if (!ticket.getCreatedBy().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("You are not allowed to access this ticket");
        }
    }

    private boolean isFinished(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.RESOLVED ||
                ticket.getStatus() == TicketStatus.CLOSED ||
                ticket.getStatus() == TicketStatus.CANCELLED;
    }

    private void validateTechnicianCanReceiveTicket(User user) {
        if (user.getRole() != UserRole.TECHNICIAN) {
            throw new BusinessRuleException("User must be a technician");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Technician must be active");
        }
    }
}
