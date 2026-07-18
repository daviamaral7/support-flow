package davi.spf.supportflow.auth.controller;

import davi.spf.supportflow.auth.dto.AuthenticatedUserResponse;
import davi.spf.supportflow.auth.dto.LoginRequest;
import davi.spf.supportflow.auth.dto.LoginResponse;
import davi.spf.supportflow.auth.security.CustomJwtAuthenticationConverter;
import davi.spf.supportflow.auth.service.AuthService;
import davi.spf.supportflow.common.config.ProjectSecurityConfig;
import davi.spf.supportflow.common.exception.UserNotActiveException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({ProjectSecurityConfig.class, CustomJwtAuthenticationConverter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldAllowLoginWithoutAuthenticationAndReturnAccessToken() throws Exception {
        LoginRequest request = loginRequest();
        LoginResponse response = new LoginResponse("access-token");

        when(authService.login(request)).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(authService).login(request);
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginCredentialsAreInvalid() throws Exception {
        LoginRequest request = loginRequest();

        when(authService.login(request)).thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authService).login(request);
    }

    @Test
    void shouldReturnBadRequestWhenLoginUserIsNotActive() throws Exception {
        LoginRequest request = loginRequest();

        when(authService.login(request)).thenThrow(new UserNotActiveException("User is not active"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Not active"))
                .andExpect(jsonPath("$.message").value("User is not active"))
                .andExpect(jsonPath("$.path").value("/auth/login"));

        verify(authService).login(request);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAuthenticatedUserToGetMe() throws Exception {
        AuthenticatedUserResponse response = authenticatedUserResponse();

        when(authService.me(any(Authentication.class))).thenReturn(response);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@supportflow.test"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(authService).me(any(Authentication.class));
    }

    @Test
    void shouldRejectUnauthenticatedUserWhenGettingMe() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).me(any());
    }

    @Test
    @WithMockUser(username = "blocked@supportflow.test", roles = "EMPLOYEE")
    void shouldReturnBadRequestWhenAuthenticatedUserIsNotActive() throws Exception {
        when(authService.me(any(Authentication.class))).thenThrow(new UserNotActiveException("User is not active"));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Not active"))
                .andExpect(jsonPath("$.message").value("User is not active"))
                .andExpect(jsonPath("$.path").value("/auth/me"));

        verify(authService).me(any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "missing@supportflow.test", roles = "EMPLOYEE")
    void shouldReturnUnauthorizedWhenAuthenticatedUserDoesNotExist() throws Exception {
        when(authService.me(any(Authentication.class))).thenThrow(new BadCredentialsException("User not found"));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());

        verify(authService).me(any(Authentication.class));
    }

    private LoginRequest loginRequest() {
        return new LoginRequest("user@supportflow.test", "raw-password");
    }

    private AuthenticatedUserResponse authenticatedUserResponse() {
        return new AuthenticatedUserResponse(
                1L,
                "admin",
                "admin@supportflow.test",
                "ADMIN",
                "ACTIVE"
        );
    }
}
