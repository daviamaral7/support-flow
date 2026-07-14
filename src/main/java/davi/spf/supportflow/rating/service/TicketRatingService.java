package davi.spf.supportflow.rating.service;

import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.rating.dto.TicketRatingRequestDTO;
import davi.spf.supportflow.rating.dto.TicketRatingResponseDTO;
import davi.spf.supportflow.rating.entity.TicketRating;
import davi.spf.supportflow.rating.mapper.TicketRatingMapper;
import davi.spf.supportflow.rating.repository.TicketRatingRepository;
import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.ticket.enums.TicketStatus;
import davi.spf.supportflow.ticket.repository.TicketRepository;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class TicketRatingService {

    private final TicketRatingRepository ticketRatingRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketRatingMapper mapper;

    public TicketRatingResponseDTO createRating(TicketRatingRequestDTO dto, Authentication authentication, Long ticketId) {
        Ticket ticket = findTicketByIdOrThrow(ticketId);

        User user = getAuthenticatedUser(authentication);

        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new BusinessRuleException("Only closed tickets can be rated");
        }

        if (!ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new BusinessRuleException("You can only rate tickets created by you");
        }

        if (ticketRatingRepository.existsByTicketId(ticketId)) {
            throw new BusinessRuleException("Ticket already rated");
        }

        String comment = dto.comment() == null ? null : dto.comment().trim();

        TicketRating rating = TicketRating.create(ticket, user, dto.score(), comment);

        TicketRating savedRating = ticketRatingRepository.save(rating);

        return mapper.toResponse(savedRating);
    }

    @Transactional(readOnly = true)
    public TicketRatingResponseDTO getRatingByTicket(Long ticketId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Ticket ticket = findTicketByIdOrThrow(ticketId);

        validateUserCanViewRating(user, ticket);

        TicketRating rating = ticketRatingRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found"));

        return mapper.toResponse(rating);
    }

    private @NonNull User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private @NonNull Ticket findTicketByIdOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private void validateUserCanViewRating(User user, Ticket ticket) {
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.TECHNICIAN) {
            return;
        }

        if (!ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to view this rating");
        }
    }
}
