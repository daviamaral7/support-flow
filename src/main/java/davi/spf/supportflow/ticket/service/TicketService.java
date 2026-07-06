package davi.spf.supportflow.ticket.service;

import davi.spf.supportflow.category.entity.Category;
import davi.spf.supportflow.category.repository.CategoryRepository;
import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.ticket.dto.TicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketResponseDTO;
import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.ticket.enums.TicketStatus;
import davi.spf.supportflow.ticket.mapper.TicketMapper;
import davi.spf.supportflow.ticket.repository.TicketRepository;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
        User createdBy = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

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
}
