package davi.spf.supportflow.rating.service;

import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.rating.dto.TicketRatingRequestDTO;
import davi.spf.supportflow.rating.dto.TicketRatingResponseDTO;
import davi.spf.supportflow.rating.entity.TicketRating;
import davi.spf.supportflow.rating.mapper.TicketRatingMapper;
import davi.spf.supportflow.rating.repository.TicketRatingRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketRatingServiceTest {

    @Mock
    private TicketRatingRepository ticketRatingRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketRatingMapper mapper;

    @InjectMocks
    private TicketRatingService ticketRatingService;

    @Test
    void shouldCreateRatingWhenAuthenticatedEmployeeCreatedClosedTicket() {
        User employee = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, TicketStatus.CLOSED);
        TicketRatingRequestDTO request = new TicketRatingRequestDTO(5, "  Great support  ");
        TicketRatingResponseDTO response = ratingResponse(10L, ticket.getId(), employee.getId());
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRatingRepository.existsByTicketId(ticket.getId())).thenReturn(false);
        when(ticketRatingRepository.save(any(TicketRating.class))).thenAnswer(invocation -> {
            TicketRating rating = invocation.getArgument(0);
            rating.setId(10L);
            return rating;
        });
        when(mapper.toResponse(any(TicketRating.class))).thenReturn(response);

        TicketRatingResponseDTO result = ticketRatingService.createRating(request, authentication, ticket.getId());

        ArgumentCaptor<TicketRating> ratingCaptor = ArgumentCaptor.forClass(TicketRating.class);
        verify(ticketRatingRepository).save(ratingCaptor.capture());
        TicketRating savedRating = ratingCaptor.getValue();

        assertThat(result).isSameAs(response);
        assertThat(savedRating.getTicket()).isSameAs(ticket);
        assertThat(savedRating.getRatedBy()).isSameAs(employee);
        assertThat(savedRating.getScore()).isEqualTo(5);
        assertThat(savedRating.getComment()).isEqualTo("Great support");
        verify(mapper).toResponse(savedRating);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenCreatingRatingForTicketThatIsNotClosed() {
        User employee = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, TicketStatus.RESOLVED);
        TicketRatingRequestDTO request = new TicketRatingRequestDTO(4, "Good");
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketRatingService.createRating(request, authentication, ticket.getId()))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRatingRepository, never()).save(any());
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenCreatingRatingForTicketCreatedByAnotherEmployee() {
        User employee = user(1L, UserRole.EMPLOYEE);
        User creator = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, creator, TicketStatus.CLOSED);
        TicketRatingRequestDTO request = new TicketRatingRequestDTO(4, "Good");
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketRatingService.createRating(request, authentication, ticket.getId()))
                .isInstanceOf(AccessDeniedException.class);

        verify(ticketRatingRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenCreatingRatingForAlreadyRatedTicket() {
        User employee = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, TicketStatus.CLOSED);
        TicketRatingRequestDTO request = new TicketRatingRequestDTO(4, "Good");
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRatingRepository.existsByTicketId(ticket.getId())).thenReturn(true);

        assertThatThrownBy(() -> ticketRatingService.createRating(request, authentication, ticket.getId()))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRatingRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCreatingRatingForUnknownTicket() {
        User employee = user(1L, UserRole.EMPLOYEE);
        TicketRatingRequestDTO request = new TicketRatingRequestDTO(4, "Good");
        Authentication authentication = mock(Authentication.class);

        when(ticketRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRatingService.createRating(request, authentication, 100L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(userRepository);
        verify(ticketRatingRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCreatingRatingWithUnknownAuthenticatedUser() {
        User employee = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, TicketStatus.CLOSED);
        TicketRatingRequestDTO request = new TicketRatingRequestDTO(4, "Good");
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(employee.getEmail());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRatingService.createRating(request, authentication, ticket.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRatingRepository, never()).save(any());
    }

    @Test
    void shouldAllowAdminToGetRatingByAnyTicket() {
        User admin = user(1L, UserRole.ADMIN);
        User creator = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, creator, TicketStatus.CLOSED);
        TicketRating rating = rating(10L, ticket, creator);
        TicketRatingResponseDTO response = ratingResponse(rating.getId(), ticket.getId(), creator.getId());
        Authentication authentication = authentication(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRatingRepository.findByTicketId(ticket.getId())).thenReturn(Optional.of(rating));
        when(mapper.toResponse(rating)).thenReturn(response);

        TicketRatingResponseDTO result = ticketRatingService.getRatingByTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldAllowTechnicianToGetRatingByAnyTicket() {
        User technician = user(1L, UserRole.TECHNICIAN);
        User creator = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, creator, TicketStatus.CLOSED);
        TicketRating rating = rating(10L, ticket, creator);
        TicketRatingResponseDTO response = ratingResponse(rating.getId(), ticket.getId(), creator.getId());
        Authentication authentication = authentication(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRatingRepository.findByTicketId(ticket.getId())).thenReturn(Optional.of(rating));
        when(mapper.toResponse(rating)).thenReturn(response);

        TicketRatingResponseDTO result = ticketRatingService.getRatingByTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldAllowCreatorEmployeeToGetRatingByOwnTicket() {
        User employee = user(1L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, employee, TicketStatus.CLOSED);
        TicketRating rating = rating(10L, ticket, employee);
        TicketRatingResponseDTO response = ratingResponse(rating.getId(), ticket.getId(), employee.getId());
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRatingRepository.findByTicketId(ticket.getId())).thenReturn(Optional.of(rating));
        when(mapper.toResponse(rating)).thenReturn(response);

        TicketRatingResponseDTO result = ticketRatingService.getRatingByTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenEmployeeGetsRatingForTicketCreatedByAnotherUser() {
        User employee = user(1L, UserRole.EMPLOYEE);
        User creator = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, creator, TicketStatus.CLOSED);
        Authentication authentication = authentication(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketRatingService.getRatingByTicket(ticket.getId(), authentication))
                .isInstanceOf(AccessDeniedException.class);

        verify(ticketRatingRepository, never()).findByTicketId(ticket.getId());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenRatingDoesNotExist() {
        User admin = user(1L, UserRole.ADMIN);
        User creator = user(2L, UserRole.EMPLOYEE);
        Ticket ticket = ticket(100L, creator, TicketStatus.CLOSED);
        Authentication authentication = authentication(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRatingRepository.findByTicketId(ticket.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRatingService.getRatingByTicket(ticket.getId(), authentication))
                .isInstanceOf(ResourceNotFoundException.class);
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

    private Ticket ticket(Long id, User createdBy, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle("Ticket " + id);
        ticket.setDescription("Ticket description");
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setStatus(status);
        ticket.setCreatedBy(createdBy);
        return ticket;
    }

    private TicketRating rating(Long id, Ticket ticket, User ratedBy) {
        TicketRating rating = TicketRating.create(ticket, ratedBy, 5, "Great support");
        rating.setId(id);
        rating.setCreatedAt(LocalDateTime.now());
        return rating;
    }

    private TicketRatingResponseDTO ratingResponse(Long id, Long ticketId, Long ratedById) {
        return new TicketRatingResponseDTO(
                id,
                ticketId,
                ratedById,
                "Rated User",
                5,
                "Great support",
                LocalDateTime.now()
        );
    }
}
