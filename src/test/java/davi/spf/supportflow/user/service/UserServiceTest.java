package davi.spf.supportflow.user.service;

import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceAlreadyExistsException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.common.exception.UserNotActiveException;
import davi.spf.supportflow.user.dto.UserRequestDTO;
import davi.spf.supportflow.user.dto.UserResponseDTO;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.enums.UserStatus;
import davi.spf.supportflow.user.mapper.UserMapper;
import davi.spf.supportflow.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldListUsersWithStatusDifferentFromDeleted() {
        Pageable pageable = PageRequest.of(0, 10);
        User activeUser = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User blockedUser = user(2L, UserRole.TECHNICIAN, UserStatus.BLOCKED);
        UserResponseDTO activeResponse = response(activeUser);
        UserResponseDTO blockedResponse = response(blockedUser);

        when(userRepository.findAllByStatusNot(UserStatus.DELETED, pageable))
                .thenReturn(new PageImpl<>(List.of(activeUser, blockedUser), pageable, 2));
        when(mapper.toResponse(activeUser)).thenReturn(activeResponse);
        when(mapper.toResponse(blockedUser)).thenReturn(blockedResponse);

        Page<UserResponseDTO> result = userService.listUsers(pageable);

        assertThat(result.getContent()).containsExactly(activeResponse, blockedResponse);
        verify(userRepository).findAllByStatusNot(UserStatus.DELETED, pageable);
        verify(mapper).toResponse(activeUser);
        verify(mapper).toResponse(blockedUser);
    }

    @Test
    void shouldReturnUserResponseWhenFindingExistingUserById() {
        User user = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        UserResponseDTO response = response(user);

        when(userRepository.findByIdAndStatusNot(user.getId(), UserStatus.DELETED)).thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(response);

        UserResponseDTO result = userService.findUserById(user.getId());

        assertThat(result).isSameAs(response);
        verify(userRepository).findByIdAndStatusNot(user.getId(), UserStatus.DELETED);
        verify(mapper).toResponse(user);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenFindingUserByIdAndUserDoesNotExist() {
        when(userRepository.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByIdAndStatusNot(99L, UserStatus.DELETED);
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldIgnoreDeletedUsersWhenFindingUserById() {
        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserById(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByIdAndStatusNot(1L, UserStatus.DELETED);
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldCreateUserWithNormalizedEmailActiveStatusAndEncryptedPassword() {
        UserRequestDTO request = request("New User", "  NEW.USER@SupportFlow.Test  ", "raw-password", UserRole.EMPLOYEE);
        User userFromMapper = user(null, UserRole.EMPLOYEE, null);
        UserResponseDTO response = response(1L, "New User", "new.user@supportflow.test", UserRole.EMPLOYEE, UserStatus.ACTIVE);

        when(userRepository.existsByEmail("new.user@supportflow.test")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(userFromMapper);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });
        when(mapper.toResponse(userFromMapper)).thenReturn(response);

        UserResponseDTO result = userService.createUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(result).isSameAs(response);
        assertThat(savedUser.getEmail()).isEqualTo("new.user@supportflow.test");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        verify(userRepository).existsByEmail("new.user@supportflow.test");
        verify(passwordEncoder).encode("raw-password");
        verify(mapper).toResponse(savedUser);
    }

    @Test
    void shouldThrowResourceAlreadyExistsExceptionWhenCreatingUserWithExistingEmail() {
        UserRequestDTO request = request("Existing User", "  EXISTING@SupportFlow.Test  ", "raw-password", UserRole.EMPLOYEE);

        when(userRepository.existsByEmail("existing@supportflow.test")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(userRepository).existsByEmail("existing@supportflow.test");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(mapper, passwordEncoder);
    }

    @Test
    void shouldBlockActiveUserChangingStatusToBlocked() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        userService.blockUser(target.getId(), authentication);

        assertThat(target.getStatus()).isEqualTo(UserStatus.BLOCKED);
        verify(userRepository).findByEmail(admin.getEmail());
        verify(userRepository).findById(target.getId());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenBlockingItself() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Authentication authentication = authentication(admin);

        assertThatThrownBy(() -> userService.blockUser(admin.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository).findByEmail(admin.getEmail());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenBlockingUnknownUser() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.blockUser(99L, authentication))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByEmail(admin.getEmail());
        verify(userRepository).findById(99L);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenBlockingAlreadyBlockedUser() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, UserRole.EMPLOYEE, UserStatus.BLOCKED);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.blockUser(target.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(target.getStatus()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenBlockingDeletedUser() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, UserRole.EMPLOYEE, UserStatus.DELETED);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.blockUser(target.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(target.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    void shouldUnblockBlockedUserChangingStatusToActive() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, UserRole.EMPLOYEE, UserStatus.BLOCKED);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        userService.unblockUser(target.getId(), authentication);

        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).findByEmail(admin.getEmail());
        verify(userRepository).findById(target.getId());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenUnblockingItself() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Authentication authentication = authentication(admin);

        assertThatThrownBy(() -> userService.unblockUser(admin.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository).findByEmail(admin.getEmail());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUnblockingUnknownUser() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.unblockUser(99L, authentication))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByEmail(admin.getEmail());
        verify(userRepository).findById(99L);
    }

    @Test
    void shouldThrowUserNotActiveExceptionWhenUnblockingAlreadyActiveUser() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.unblockUser(target.getId(), authentication))
                .isInstanceOf(UserNotActiveException.class);

        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldThrowUserNotActiveExceptionWhenUnblockingDeletedUser() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, UserRole.EMPLOYEE, UserStatus.DELETED);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.unblockUser(target.getId(), authentication))
                .isInstanceOf(UserNotActiveException.class);

        assertThat(target.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    void shouldSoftDeleteUserChangingStatusToDeleted() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        userService.deleteUser(target.getId(), authentication);

        assertThat(target.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(userRepository).findByEmail(admin.getEmail());
        verify(userRepository).findById(target.getId());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenDeletingItself() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Authentication authentication = authentication(admin);

        assertThatThrownBy(() -> userService.deleteUser(admin.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository).findByEmail(admin.getEmail());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenDeletingUnknownUser() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L, authentication))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByEmail(admin.getEmail());
        verify(userRepository).findById(99L);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenDeletingAlreadyDeletedUser() {
        User admin = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, UserRole.EMPLOYEE, UserStatus.DELETED);
        Authentication authentication = authentication(admin);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.deleteUser(target.getId(), authentication))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(target.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(userRepository).findByEmail(admin.getEmail());
    }

    private UserRequestDTO request(String name, String email, String password, UserRole role) {
        return new UserRequestDTO(name, email, password, role);
    }

    private UserResponseDTO response(User user) {
        return response(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus());
    }

    private UserResponseDTO response(Long id, String name, String email, UserRole role, UserStatus status) {
        return new UserResponseDTO(
                id,
                name,
                email,
                role == null ? null : role.name(),
                status == null ? null : status.name(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private Authentication authentication(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return authentication;
    }

    private User user(Long id, UserRole role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setName(role == null ? "user" : role.name().toLowerCase() + "-" + id);
        user.setEmail(id == null ? "new.user@supportflow.test" : "user" + id + "@supportflow.test");
        user.setPassword("encrypted-password-" + id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
