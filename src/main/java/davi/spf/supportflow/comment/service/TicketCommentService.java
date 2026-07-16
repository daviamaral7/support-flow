package davi.spf.supportflow.comment.service;

import davi.spf.supportflow.comment.dto.TicketCommentResponseDTO;
import davi.spf.supportflow.comment.entity.TicketComment;
import davi.spf.supportflow.comment.mapper.TicketCommentMapper;
import davi.spf.supportflow.comment.repository.TicketCommentRepository;
import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.ticket.enums.TicketStatus;
import davi.spf.supportflow.ticket.repository.TicketRepository;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class TicketCommentService {

    private final TicketCommentRepository ticketCommentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketCommentMapper mapper;

    public TicketCommentResponseDTO makeComment(Long ticketId, String message, Authentication authentication) {
        Ticket ticket = findTicketByIdOrThrow(ticketId);
        User author = getAuthenticatedUser(authentication);

        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("Closed or cancelled tickets cannot receive comments");
        }

        validateUserCanComment(ticket, author);

        TicketComment comment = TicketComment.create(ticket, author, message);

        TicketComment savedComment = ticketCommentRepository.save(comment);

        return mapper.toResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public Page<TicketCommentResponseDTO> listComments(Long ticketId,
                                                       Authentication authentication,
                                                       Pageable pageable) {

        Ticket ticket = findTicketByIdOrThrow(ticketId);
        User user = getAuthenticatedUser(authentication);

        validateCommentAccess(ticket, user);

        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId, pageable)
                .map(mapper::toResponse);
    }

    private Ticket findTicketByIdOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void validateUserCanComment(Ticket ticket, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (user.getRole() == UserRole.TECHNICIAN) {
            if (ticket.getAssignedTo() == null || !ticket.getAssignedTo().getId().equals(user.getId())) {
                throw new BusinessRuleException("Only the technician assigned to the ticket can comment");
            }

            return;
        }

        if (user.getRole() == UserRole.EMPLOYEE) {
            if (!ticket.getCreatedBy().getId().equals(user.getId())) {
                throw new BusinessRuleException("You are not the creator of this ticket");
            }

            return;
        }

        throw new BusinessRuleException("User cannot comment on this ticket");
    }

    private void validateCommentAccess(Ticket ticket, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (user.getRole() == UserRole.TECHNICIAN) {
            if (ticket.getAssignedTo() == null || !ticket.getAssignedTo().getId().equals(user.getId())) {
                throw new BusinessRuleException("You are not allowed to access comments for this ticket");
            }

            return;
        }

        if (!ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new BusinessRuleException("You are not allowed to access comments for this ticket");
        }
    }
}
