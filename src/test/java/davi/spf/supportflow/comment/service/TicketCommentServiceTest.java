package davi.spf.supportflow.comment.service;

import davi.spf.supportflow.comment.dto.TicketCommentResponseDTO;
import davi.spf.supportflow.comment.entity.TicketComment;
import davi.spf.supportflow.comment.mapper.TicketCommentMapper;
import davi.spf.supportflow.comment.repository.TicketCommentRepository;
import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.ticket.enums.TicketPriority;
import davi.spf.supportflow.ticket.enums.TicketStatus;
import davi.spf.supportflow.ticket.repository.TicketRepository;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.enums.UserStatus;
import davi.spf.supportflow.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketCommentServiceTest {

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketCommentMapper mapper;

    @InjectMocks
    private TicketCommentService ticketCommentService;

    @Test
    void shouldCreateCommentWhenAdminCommentsOnAnyAllowedTicket() {
        User admin = user(1L, UserRole.ADMIN);
        User employee = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        TicketCommentResponseDTO response = commentResponse(10L, ticket.getId(), admin.getId(), "Admin note");
        Authentication authentication = authentication(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> {
            TicketComment comment = invocation.getArgument(0);
            comment.setId(10L);
            return comment;
        });
        when(mapper.toResponse(any(TicketComment.class))).thenReturn(response);

        TicketCommentResponseDTO result = ticketCommentService.makeComment(ticket.getId(), "Admin note", authentication);

        ArgumentCaptor<TicketComment> commentCaptor = ArgumentCaptor.forClass(TicketComment.class);
        verify(ticketCommentRepository).save(commentCaptor.capture());
        TicketComment savedComment = commentCaptor.getValue();

        assertThat(result).isSameAs(response);
        assertThat(savedComment.getTicket()).isSameAs(ticket);
        assertThat(savedComment.getAuthor()).isSameAs(admin);
        assertThat(savedComment.getMessage()).isEqualTo("Admin note");
        verify(mapper).toResponse(savedComment);
    }

    @Test
    void shouldCreateCommentWhenTechnicianCommentsOnAssignedTicket() {
        User technician = user(1L, UserRole.TECHNICIAN);
        User employee = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, technician, TicketStatus.IN_PROGRESS);
        TicketCommentResponseDTO response = commentResponse(10L, ticket.getId(), technician.getId(), "Technician note");
        Authentication authentication = authentication(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(TicketComment.class))).thenReturn(response);

        TicketCommentResponseDTO result = ticketCommentService.makeComment(ticket.getId(), "Technician note", authentication);

        ArgumentCaptor<TicketComment> commentCaptor = ArgumentCaptor.forClass(TicketComment.class);
        verify(ticketCommentRepository).save(commentCaptor.capture());
        TicketComment savedComment = commentCaptor.getValue();

        assertThat(result).isSameAs(response);
        assertThat(savedComment.getTicket()).isSameAs(ticket);
        assertThat(savedComment.getAuthor()).isSameAs(technician);
        assertThat(savedComment.getMessage()).isEqualTo("Technician note");
    }

    @Test
    void shouldCreateCommentWhenEmployeeCommentsOnOwnTicket() {
        User employee = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        TicketCommentResponseDTO response = commentResponse(10L, ticket.getId(), employee.getId(), "Employee note");
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(TicketComment.class))).thenReturn(response);

        TicketCommentResponseDTO result = ticketCommentService.makeComment(ticket.getId(), "Employee note", authentication);

        ArgumentCaptor<TicketComment> commentCaptor = ArgumentCaptor.forClass(TicketComment.class);
        verify(ticketCommentRepository).save(commentCaptor.capture());
        TicketComment savedComment = commentCaptor.getValue();

        assertThat(result).isSameAs(response);
        assertThat(savedComment.getTicket()).isSameAs(ticket);
        assertThat(savedComment.getAuthor()).isSameAs(employee);
        assertThat(savedComment.getMessage()).isEqualTo("Employee note");
    }

    @Test
    void shouldSaveCommentMessageAsReceivedWhenServiceDoesNotTrim() {
        User employee = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(TicketComment.class)))
                .thenReturn(commentResponse(10L, ticket.getId(), employee.getId(), "  Employee note  "));

        ticketCommentService.makeComment(ticket.getId(), "  Employee note  ", authentication);

        ArgumentCaptor<TicketComment> commentCaptor = ArgumentCaptor.forClass(TicketComment.class);
        verify(ticketCommentRepository).save(commentCaptor.capture());

        assertThat(commentCaptor.getValue().getMessage()).isEqualTo("  Employee note  ");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCreatingCommentForUnknownTicket() {
        Authentication authentication = mock(Authentication.class);

        when(ticketRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketCommentService.makeComment(100L, "Comment", authentication))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(userRepository);
        verify(ticketCommentRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCreatingCommentWithUnknownAuthenticatedUser() {
        User employee = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(employee.getEmail());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketCommentService.makeComment(ticket.getId(), "Comment", authentication))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketCommentRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenTechnicianCommentsOnTicketNotAssignedToHim() {
        User technician = user(1L, UserRole.TECHNICIAN);
        User anotherTechnician = user(2L, UserRole.TECHNICIAN);
        User employee = user(3L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, anotherTechnician, TicketStatus.IN_PROGRESS);
        Authentication authentication = authentication(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketCommentService.makeComment(ticket.getId(), "Comment", authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketCommentRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenEmployeeCommentsOnTicketCreatedByAnotherUser() {
        User employee = user(1L, UserRole.EMPLOYEE);
        User creator = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, creator, null, TicketStatus.OPEN);
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketCommentService.makeComment(ticket.getId(), "Comment", authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketCommentRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"CLOSED", "CANCELLED"})
    void shouldThrowBusinessRuleExceptionWhenCreatingCommentForClosedOrCancelledTicket(TicketStatus status) {
        User admin = user(1L, UserRole.ADMIN);
        User employee = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, null, status);
        Authentication authentication = authentication(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketCommentService.makeComment(ticket.getId(), "Comment", authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketCommentRepository, never()).save(any());
    }

    @Test
    void shouldAllowAdminToListCommentsForAnyTicket() {
        User admin = user(1L, UserRole.ADMIN);
        User employee = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        Pageable pageable = PageRequest.of(0, 10);
        TicketComment comment1 = comment(10L, ticket, employee, "First");
        TicketComment comment2 = comment(11L, ticket, admin, "Second");
        TicketCommentResponseDTO response1 = commentResponse(comment1.getId(), ticket.getId(), employee.getId(), "First");
        TicketCommentResponseDTO response2 = commentResponse(comment2.getId(), ticket.getId(), admin.getId(), "Second");
        Authentication authentication = authentication(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(comment1, comment2), pageable, 2));
        when(mapper.toResponse(comment1)).thenReturn(response1);
        when(mapper.toResponse(comment2)).thenReturn(response2);

        Page<TicketCommentResponseDTO> result = ticketCommentService.listComments(ticket.getId(), authentication, pageable);

        assertThat(result.getContent()).containsExactly(response1, response2);
        verify(mapper).toResponse(comment1);
        verify(mapper).toResponse(comment2);
    }

    @Test
    void shouldAllowAssignedTechnicianToListCommentsForTicketCreatedByEmployee() {
        User technician = user(1L, UserRole.TECHNICIAN);
        User employee = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, technician, TicketStatus.IN_PROGRESS);
        Pageable pageable = PageRequest.of(0, 10);
        TicketComment comment = comment(10L, ticket, technician, "Technician comment");
        TicketCommentResponseDTO response = commentResponse(comment.getId(), ticket.getId(), technician.getId(), "Technician comment");
        Authentication authentication = authentication(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));
        when(mapper.toResponse(comment)).thenReturn(response);

        Page<TicketCommentResponseDTO> result = ticketCommentService.listComments(ticket.getId(), authentication, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenTechnicianListsCommentsForTicketNotAssignedToHim() {
        User technician = user(1L, UserRole.TECHNICIAN);
        User anotherTechnician = user(2L, UserRole.TECHNICIAN);
        User employee = user(3L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, anotherTechnician, TicketStatus.IN_PROGRESS);
        Pageable pageable = PageRequest.of(0, 10);
        Authentication authentication = authentication(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketCommentService.listComments(ticket.getId(), authentication, pageable))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketCommentRepository, never()).findByTicketIdOrderByCreatedAtAsc(ticket.getId(), pageable);
    }

    @Test
    void shouldAllowEmployeeToListCommentsForOwnTicket() {
        User employee = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        Pageable pageable = PageRequest.of(0, 10);
        TicketComment comment = comment(10L, ticket, employee, "Employee comment");
        TicketCommentResponseDTO response = commentResponse(comment.getId(), ticket.getId(), employee.getId(), "Employee comment");
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));
        when(mapper.toResponse(comment)).thenReturn(response);

        Page<TicketCommentResponseDTO> result = ticketCommentService.listComments(ticket.getId(), authentication, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenEmployeeListsCommentsForTicketCreatedByAnotherUser() {
        User employee = user(1L, UserRole.EMPLOYEE);
        User creator = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, creator, null, TicketStatus.OPEN);
        Pageable pageable = PageRequest.of(0, 10);
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketCommentService.listComments(ticket.getId(), authentication, pageable))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketCommentRepository, never()).findByTicketIdOrderByCreatedAtAsc(ticket.getId(), pageable);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenListingCommentsForUnknownTicket() {
        Pageable pageable = PageRequest.of(0, 10);
        Authentication authentication = mock(Authentication.class);

        when(ticketRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketCommentService.listComments(100L, authentication, pageable))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(userRepository);
        verify(ticketCommentRepository, never()).findByTicketIdOrderByCreatedAtAsc(100L, pageable);
    }

    private Authentication authentication(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return authentication;
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName(role.name().toLowerCase() + "-" + id);
        user.setEmail("user" + id + "@supportflow.test");
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Ticket ticket(Long id, User createdBy, User assignedTo, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle("Ticket " + id);
        ticket.setDescription("Ticket description");
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setStatus(status);
        ticket.setCreatedBy(createdBy);
        ticket.setAssignedTo(assignedTo);
        return ticket;
    }

    private TicketComment comment(Long id, Ticket ticket, User author, String message) {
        TicketComment comment = TicketComment.create(ticket, author, message);
        comment.setId(id);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }

    private TicketCommentResponseDTO commentResponse(Long id, Long ticketId, Long authorId, String message) {
        return new TicketCommentResponseDTO(
                id,
                ticketId,
                authorId,
                "Author " + authorId,
                UserRole.EMPLOYEE.name(),
                message,
                LocalDateTime.now()
        );
    }
}
