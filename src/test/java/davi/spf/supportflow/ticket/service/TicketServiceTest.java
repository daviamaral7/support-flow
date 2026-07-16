package davi.spf.supportflow.ticket.service;

import davi.spf.supportflow.category.entity.Category;
import davi.spf.supportflow.category.repository.CategoryRepository;
import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.history.service.TicketHistoryService;
import davi.spf.supportflow.ticket.dto.AssignTicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketFilterDTO;
import davi.spf.supportflow.ticket.dto.TicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketResponseDTO;
import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.ticket.enums.TicketPriority;
import davi.spf.supportflow.ticket.enums.TicketStatus;
import davi.spf.supportflow.ticket.mapper.TicketMapper;
import davi.spf.supportflow.ticket.repository.TicketRepository;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.enums.UserStatus;
import davi.spf.supportflow.user.repository.UserRepository;
import jakarta.persistence.criteria.*;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TicketMapper mapper;

    @Mock
    private TicketHistoryService ticketHistoryService;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void shouldCreateTicketWithOpenStatusActiveCategoryAuthenticatedCreatorAndNoAssigneeOrClosedAt() {
        User employee = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Category category = category(10L, true);
        TicketRequestDTO request = ticketRequest(category.getId());
        Ticket ticketFromMapper = ticket(null, employee, user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE), TicketStatus.CLOSED);
        ticketFromMapper.setClosedAt(LocalDateTime.now());
        TicketResponseDTO response = ticketResponse(ticketFromMapper);
        Authentication authentication = stubAuthenticated(employee);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(mapper.toEntity(request)).thenReturn(ticketFromMapper);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(100L);
            return ticket;
        });
        when(mapper.toResponse(any(Ticket.class))).thenReturn(response);

        TicketResponseDTO result = ticketService.createTicket(request, authentication);

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();

        assertThat(result).isSameAs(response);
        assertThat(savedTicket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(savedTicket.getCategory()).isSameAs(category);
        assertThat(savedTicket.getCreatedBy()).isSameAs(employee);
        assertThat(savedTicket.getAssignedTo()).isNull();
        assertThat(savedTicket.getClosedAt()).isNull();
        verify(ticketHistoryService).registerCreated(savedTicket, employee);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCreatingTicketWithUnknownCategory() {
        User employee = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        TicketRequestDTO request = ticketRequest(99L);
        Authentication authentication = stubAuthenticated(employee);

        when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.createTicket(request, authentication))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenCreatingTicketWithInactiveCategory() {
        User employee = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Category inactiveCategory = category(10L, false);
        TicketRequestDTO request = ticketRequest(inactiveCategory.getId());
        Authentication authentication = stubAuthenticated(employee);

        when(categoryRepository.findById(inactiveCategory.getId())).thenReturn(Optional.of(inactiveCategory));

        assertThatThrownBy(() -> ticketService.createTicket(request, authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldListAllTicketsWhenAuthenticatedUserIsAdmin() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Authentication authentication = stubAuthenticated(admin);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = ticket(100L, admin, null, TicketStatus.OPEN);
        TicketResponseDTO response = ticketResponse(ticket);

        when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(ticket), pageable, 1));
        when(mapper.toResponse(ticket)).thenReturn(response);

        Page<TicketResponseDTO> result = ticketService.listTickets(emptyFilter(), authentication, pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(ticketRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldListAllTicketsWhenAuthenticatedUserIsTechnician() {
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        Authentication authentication = stubAuthenticated(technician);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), technician, TicketStatus.IN_PROGRESS);
        TicketResponseDTO response = ticketResponse(ticket);

        when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(ticket), pageable, 1));
        when(mapper.toResponse(ticket)).thenReturn(response);

        Page<TicketResponseDTO> result = ticketService.listTickets(emptyFilter(), authentication, pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(ticketRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldListOnlyTicketsCreatedByEmployee() {
        User employee = user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Authentication authentication = stubAuthenticated(employee);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        TicketResponseDTO response = ticketResponse(ticket);

        when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(ticket), pageable, 1));
        when(mapper.toResponse(ticket)).thenReturn(response);

        Page<TicketResponseDTO> result = ticketService.listTickets(emptyFilter(), authentication, pageable);

        assertThat(result.getContent()).containsExactly(response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Ticket>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(ticketRepository).findAll(specCaptor.capture(), eq(pageable));

        Root<Ticket> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> createdByPath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("createdBy")).thenReturn(createdByPath);
        when(createdByPath.get("id")).thenReturn(idPath);
        when(criteriaBuilder.equal(idPath, employee.getId())).thenReturn(predicate);

        assertThat(specCaptor.getValue().toPredicate(root, query, criteriaBuilder)).isSameAs(predicate);
        verify(criteriaBuilder).equal(idPath, employee.getId());
    }

    @Test
    void shouldAllowAdminToGetAnyTicketById() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User employee = user(2L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticatedWithRoles(admin, "ADMIN");

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.getTicketById(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldAllowTechnicianToGetAnyTicketById() {
        User technician = user(1L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        User employee = user(2L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticatedWithRoles(technician, "TECHNICIAN");

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.getTicketById(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldAllowEmployeeToGetTicketCreatedByHim() {
        User employee = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticatedWithRoles(employee, "EMPLOYEE");

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.getTicketById(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenEmployeeGetsTicketCreatedByAnotherUser() {
        User employee = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        User anotherEmployee = user(2L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, anotherEmployee, null, TicketStatus.OPEN);
        Authentication authentication = stubAuthenticatedWithRoles(employee, "EMPLOYEE");

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.getTicketById(ticket.getId(), authentication))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAssignActiveTechnicianToTicket() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, TicketStatus.OPEN);
        AssignTicketRequestDTO request = new AssignTicketRequestDTO(technician.getId());
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticated(admin);

        when(userRepository.findById(technician.getId())).thenReturn(Optional.of(technician));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.assignTicket(request, ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
        assertThat(ticket.getAssignedTo()).isSameAs(technician);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(ticketHistoryService).registerAssigned(ticket, admin, technician);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenAssigningUnknownTechnician() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        AssignTicketRequestDTO request = new AssignTicketRequestDTO(99L);
        Authentication authentication = stubAuthenticated(admin);

        when(userRepository.findById(request.technicianId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.assignTicket(request, 100L, authentication))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).findById(any());
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenAssigningUserThatIsNotTechnician() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User employee = user(2L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.OPEN);
        AssignTicketRequestDTO request = new AssignTicketRequestDTO(employee.getId());
        Authentication authentication = stubAuthenticated(admin);

        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.assignTicket(request, ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getAssignedTo()).isNull();
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenAssigningInactiveTechnician() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.BLOCKED);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, TicketStatus.OPEN);
        AssignTicketRequestDTO request = new AssignTicketRequestDTO(technician.getId());
        Authentication authentication = stubAuthenticated(admin);

        when(userRepository.findById(technician.getId())).thenReturn(Optional.of(technician));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.assignTicket(request, ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getAssignedTo()).isNull();
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenAssigningTicketToSameTechnician() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), technician, TicketStatus.IN_PROGRESS);
        AssignTicketRequestDTO request = new AssignTicketRequestDTO(technician.getId());
        Authentication authentication = stubAuthenticated(admin);

        when(userRepository.findById(technician.getId())).thenReturn(Optional.of(technician));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.assignTicket(request, ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(ticketHistoryService);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"RESOLVED", "CLOSED", "CANCELLED"})
    void shouldThrowBusinessRuleExceptionWhenAssigningFinishedTicket(TicketStatus status) {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, status);
        AssignTicketRequestDTO request = new AssignTicketRequestDTO(technician.getId());
        Authentication authentication = stubAuthenticated(admin);

        when(userRepository.findById(technician.getId())).thenReturn(Optional.of(technician));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.assignTicket(request, ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getAssignedTo()).isNull();
        assertThat(ticket.getStatus()).isEqualTo(status);
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldAllowAuthenticatedTechnicianToClaimFreeTicket() {
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, TicketStatus.OPEN);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticated(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.claimTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
        assertThat(ticket.getAssignedTo()).isSameAs(technician);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(ticketHistoryService).registerClaimed(ticket, technician);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenClaimingAlreadyAssignedTicket() {
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        User assignedTechnician = user(4L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), assignedTechnician, TicketStatus.IN_PROGRESS);
        Authentication authentication = stubAuthenticated(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.claimTicket(ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getAssignedTo()).isSameAs(assignedTechnician);
        verifyNoInteractions(ticketHistoryService);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"RESOLVED", "CLOSED", "CANCELLED"})
    void shouldThrowBusinessRuleExceptionWhenClaimingFinishedTicket(TicketStatus status) {
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, status);
        Authentication authentication = stubAuthenticated(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.claimTicket(ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getAssignedTo()).isNull();
        assertThat(ticket.getStatus()).isEqualTo(status);
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenClaimingWithAuthenticatedUserThatIsNotTechnician() {
        User employee = user(2L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Authentication authentication = stubAuthenticated(employee);

        assertThatThrownBy(() -> ticketService.claimTicket(100L, authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).findById(any());
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenClaimingWithInactiveAuthenticatedTechnician() {
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.BLOCKED);
        Authentication authentication = stubAuthenticated(technician);

        assertThatThrownBy(() -> ticketService.claimTicket(100L, authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).findById(any());
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldAllowAdminToResolveInProgressTicket() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, TicketStatus.IN_PROGRESS);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticated(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.resolveTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        verify(ticketHistoryService).registerResolved(ticket, admin);
    }

    @Test
    void shouldAllowAssignedTechnicianToResolveInProgressTicket() {
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), technician, TicketStatus.IN_PROGRESS);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticated(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.resolveTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        verify(ticketHistoryService).registerResolved(ticket, technician);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenTechnicianResolvesTicketAssignedToAnotherTechnician() {
        User technician = user(2L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        User anotherTechnician = user(4L, UserRole.TECHNICIAN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), anotherTechnician, TicketStatus.IN_PROGRESS);
        Authentication authentication = stubAuthenticated(technician);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.resolveTicket(ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verifyNoInteractions(ticketHistoryService);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "RESOLVED", "CLOSED", "CANCELLED"})
    void shouldThrowBusinessRuleExceptionWhenResolvingTicketThatIsNotInProgress(TicketStatus status) {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, status);
        Authentication authentication = stubAuthenticated(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.resolveTicket(ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getStatus()).isEqualTo(status);
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldAllowAdminToCloseResolvedTicket() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, TicketStatus.RESOLVED);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticated(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.closeTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getClosedAt()).isNotNull();
        verify(ticketHistoryService).registerClosed(ticket, admin);
    }

    @Test
    void shouldAllowCreatorEmployeeToCloseResolvedTicket() {
        User employee = user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.RESOLVED);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticated(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.closeTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getClosedAt()).isNotNull();
        verify(ticketHistoryService).registerClosed(ticket, employee);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenEmployeeClosesTicketCreatedByAnotherUser() {
        User employee = user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        User creator = user(4L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, creator, null, TicketStatus.RESOLVED);
        Authentication authentication = stubAuthenticated(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.closeTicket(ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getClosedAt()).isNull();
        verifyNoInteractions(ticketHistoryService);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "CLOSED", "CANCELLED"})
    void shouldThrowBusinessRuleExceptionWhenClosingTicketThatIsNotResolved(TicketStatus status) {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, status);
        Authentication authentication = stubAuthenticated(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.closeTicket(ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getStatus()).isEqualTo(status);
        verifyNoInteractions(ticketHistoryService);
    }

    @Test
    void shouldAllowAdminToCancelAllowedTicket() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, TicketStatus.OPEN);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticated(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.cancelTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(ticket.getClosedAt()).isNotNull();
        verify(ticketHistoryService).registerCancelled(ticket, admin);
    }

    @Test
    void shouldAllowCreatorEmployeeToCancelAllowedTicket() {
        User employee = user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, employee, null, TicketStatus.IN_PROGRESS);
        TicketResponseDTO response = ticketResponse(ticket);
        Authentication authentication = stubAuthenticated(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        TicketResponseDTO result = ticketService.cancelTicket(ticket.getId(), authentication);

        assertThat(result).isSameAs(response);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(ticket.getClosedAt()).isNotNull();
        verify(ticketHistoryService).registerCancelled(ticket, employee);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenEmployeeCancelsTicketCreatedByAnotherUser() {
        User employee = user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        User creator = user(4L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, creator, null, TicketStatus.OPEN);
        Authentication authentication = stubAuthenticated(employee);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.cancelTicket(ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getClosedAt()).isNull();
        verifyNoInteractions(ticketHistoryService);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"RESOLVED", "CLOSED", "CANCELLED"})
    void shouldThrowBusinessRuleExceptionWhenCancellingFinishedTicket(TicketStatus status) {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Ticket ticket = ticket(100L, user(3L, UserRole.EMPLOYEE, UserStatus.ACTIVE), null, status);
        Authentication authentication = stubAuthenticated(admin);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.cancelTicket(ticket.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(ticket.getStatus()).isEqualTo(status);
        verifyNoInteractions(ticketHistoryService);
    }

    private Authentication stubAuthenticated(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return authentication;
    }

    private Authentication stubAuthenticatedWithRoles(User user, String... roles) {
        List<GrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());
        doReturn(authorities).when(authentication).getAuthorities();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return authentication;
    }

    private TicketRequestDTO ticketRequest(Long categoryId) {
        return new TicketRequestDTO("Printer issue", "Printer is not working", TicketPriority.HIGH, categoryId);
    }

    private TicketFilterDTO emptyFilter() {
        return new TicketFilterDTO(null, null, null, null, null);
    }

    private User user(Long id, UserRole role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setName(role.name().toLowerCase() + "-" + id);
        user.setEmail("user" + id + "@supportflow.test");
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private Category category(Long id, boolean active) {
        Category category = new Category();
        category.setId(id);
        category.setName("Category " + id);
        category.setDescription("Category description");
        category.setActive(active);
        return category;
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
        ticket.setCategory(category(10L, true));
        return ticket;
    }

    private TicketResponseDTO ticketResponse(Ticket ticket) {
        return new TicketResponseDTO(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus() == null ? null : ticket.getStatus().name(),
                ticket.getPriority() == null ? null : ticket.getPriority().name(),
                null,
                null,
                null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getClosedAt()
        );
    }
}
