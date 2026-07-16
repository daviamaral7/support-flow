package davi.spf.supportflow.auth.service;

import davi.spf.supportflow.auth.dto.AuthenticatedUserResponse;
import davi.spf.supportflow.auth.dto.LoginRequest;
import davi.spf.supportflow.auth.dto.LoginResponse;
import davi.spf.supportflow.common.exception.UserNotActiveException;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.enums.UserStatus;
import davi.spf.supportflow.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldReturnLoginResponseWithAccessTokenWhenEmailAndPasswordAreValidAndUserIsActive() {
        User user = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        LoginRequest request = loginRequest(user.getEmail(), "raw-password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-token");

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void shouldNormalizeEmailWhenLoggingIn() {
        User user = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        LoginRequest request = loginRequest("  USER1@SUPPORTFLOW.TEST  ", "raw-password");

        when(userRepository.findByEmail("user1@supportflow.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-token");

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userRepository).findByEmail("user1@supportflow.test");
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenEmailDoesNotExist() {
        LoginRequest request = loginRequest("missing@supportflow.test", "raw-password");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenPasswordIsIncorrect() {
        User user = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        LoginRequest request = loginRequest(user.getEmail(), "wrong-password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(user);
    }

    @Test
    void shouldThrowUserNotActiveExceptionWhenUserIsBlocked() {
        User user = user(1L, UserRole.EMPLOYEE, UserStatus.BLOCKED);
        LoginRequest request = loginRequest(user.getEmail(), "raw-password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserNotActiveException.class);

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void shouldThrowUserNotActiveExceptionWhenUserIsDeleted() {
        User user = user(1L, UserRole.EMPLOYEE, UserStatus.DELETED);
        LoginRequest request = loginRequest(user.getEmail(), "raw-password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserNotActiveException.class);

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void shouldCallPasswordEncoderMatchesWithSubmittedPasswordAndEncryptedPassword() {
        User user = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        LoginRequest request = loginRequest(user.getEmail(), "raw-password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-token");

        authService.login(request);

        verify(passwordEncoder).matches("raw-password", "encrypted-password-1");
    }

    @Test
    void shouldCallJwtServiceGenerateTokenWithAuthenticatedUser() {
        User user = user(1L, UserRole.EMPLOYEE, UserStatus.ACTIVE);
        LoginRequest request = loginRequest(user.getEmail(), "raw-password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-token");

        authService.login(request);

        verify(jwtService).generateToken(user);
    }

    @Test
    void shouldReturnAuthenticatedUserResponseWithAuthenticatedUserData() {
        User user = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
        Authentication authentication = authentication(user);

        AuthenticatedUserResponse response = authService.me(authentication);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.name()).isEqualTo(user.getName());
        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(response.role()).isEqualTo(UserRole.ADMIN.name());
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE.name());
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenAuthenticatedUserDoesNotExist() {
        String email = "missing@supportflow.test";
        Authentication authentication = authentication(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(authentication))
                .isInstanceOf(BadCredentialsException.class);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"BLOCKED", "DELETED"})
    void shouldThrowUserNotActiveExceptionWhenAuthenticatedUserIsNotActive(UserStatus status) {
        User user = user(1L, UserRole.EMPLOYEE, status);
        Authentication authentication = authentication(user);

        assertThatThrownBy(() -> authService.me(authentication))
                .isInstanceOf(UserNotActiveException.class);
    }

    private LoginRequest loginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }

    private Authentication authentication(User user) {
        Authentication authentication = authentication(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return authentication;
    }

    private Authentication authentication(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        return authentication;
    }

    private User user(Long id, UserRole role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setName(role.name().toLowerCase() + "-" + id);
        user.setEmail("user" + id + "@supportflow.test");
        user.setPassword("encrypted-password-" + id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
