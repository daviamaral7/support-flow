package davi.spf.supportflow.user.controller;

import davi.spf.supportflow.auth.security.CustomJwtAuthenticationConverter;
import davi.spf.supportflow.common.config.ProjectSecurityConfig;
import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceAlreadyExistsException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.common.exception.UserNotActiveException;
import davi.spf.supportflow.user.dto.UserRequestDTO;
import davi.spf.supportflow.user.dto.UserResponseDTO;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({ProjectSecurityConfig.class, CustomJwtAuthenticationConverter.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToCreateUser() throws Exception {
        UserRequestDTO request = userRequest();
        UserResponseDTO response = userResponse(1L, "New User", "new.user@supportflow.test", "EMPLOYEE", "ACTIVE");

        when(userService.createUser(request)).thenReturn(response);

        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("new.user@supportflow.test"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).createUser(request);
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeCreateUser() throws Exception {
        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest())))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianCreateUser() throws Exception {
        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest())))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any());
    }

    @Test
    void shouldRejectUnauthenticatedCreateUser() throws Exception {
        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest())))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnConflictWhenCreatingUserWithExistingEmail() throws Exception {
        UserRequestDTO request = userRequest();

        when(userService.createUser(request)).thenThrow(new ResourceAlreadyExistsException("Email already in use"));

        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Resource Conflict"))
                .andExpect(jsonPath("$.message").value("Email already in use"))
                .andExpect(jsonPath("$.path").value("/users/signup"));

        verify(userService).createUser(request);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToListUsers() throws Exception {
        Page<UserResponseDTO> response = new PageImpl<>(List.of(
                userResponse(1L, "Admin", "admin@supportflow.test", "ADMIN", "ACTIVE")
        ));

        when(userService.listUsers(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Admin"))
                .andExpect(jsonPath("$.content[0].email").value("admin@supportflow.test"))
                .andExpect(jsonPath("$.content[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        verify(userService).listUsers(any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());

        verify(userService, never()).listUsers(any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());

        verify(userService, never()).listUsers(any());
    }

    @Test
    void shouldRejectUnauthenticatedListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).listUsers(any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToFindUserById() throws Exception {
        UserResponseDTO response = userResponse(1L, "Employee", "employee@supportflow.test", "EMPLOYEE", "ACTIVE");

        when(userService.findUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Employee"))
                .andExpect(jsonPath("$.email").value("employee@supportflow.test"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).findUserById(1L);
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeFindUserById() throws Exception {
        mockMvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isForbidden());

        verify(userService, never()).findUserById(any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianFindUserById() throws Exception {
        mockMvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isForbidden());

        verify(userService, never()).findUserById(any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnNotFoundWhenFindingUnknownUserById() throws Exception {
        when(userService.findUserById(99L)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.path").value("/users/99"));

        verify(userService).findUserById(99L);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToBlockUser() throws Exception {
        mockMvc.perform(post("/users/{id}/block", 1L))
                .andExpect(status().isNoContent());

        verify(userService).blockUser(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeBlockUser() throws Exception {
        mockMvc.perform(post("/users/{id}/block", 1L))
                .andExpect(status().isForbidden());

        verify(userService, never()).blockUser(any(), any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianBlockUser() throws Exception {
        mockMvc.perform(post("/users/{id}/block", 1L))
                .andExpect(status().isForbidden());

        verify(userService, never()).blockUser(any(), any());
    }

    @Test
    void shouldRejectUnauthenticatedBlockUser() throws Exception {
        mockMvc.perform(post("/users/{id}/block", 1L))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).blockUser(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnUnprocessableEntityWhenBlockUserBreaksBusinessRule() throws Exception {
        doThrow(new BusinessRuleException("Cannot self block"))
                .when(userService).blockUser(eq(1L), any(Authentication.class));

        mockMvc.perform(post("/users/{id}/block", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Cannot self block"))
                .andExpect(jsonPath("$.path").value("/users/1/block"));

        verify(userService).blockUser(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToUnblockUser() throws Exception {
        mockMvc.perform(post("/users/{id}/unblock", 1L))
                .andExpect(status().isNoContent());

        verify(userService).unblockUser(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeUnblockUser() throws Exception {
        mockMvc.perform(post("/users/{id}/unblock", 1L))
                .andExpect(status().isForbidden());

        verify(userService, never()).unblockUser(any(), any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianUnblockUser() throws Exception {
        mockMvc.perform(post("/users/{id}/unblock", 1L))
                .andExpect(status().isForbidden());

        verify(userService, never()).unblockUser(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnBadRequestWhenUnblockUserIsNotBlocked() throws Exception {
        doThrow(new UserNotActiveException("User is not blocked"))
                .when(userService).unblockUser(eq(1L), any(Authentication.class));

        mockMvc.perform(post("/users/{id}/unblock", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Not active"))
                .andExpect(jsonPath("$.message").value("User is not blocked"))
                .andExpect(jsonPath("$.path").value("/users/1/unblock"));

        verify(userService).unblockUser(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnUnprocessableEntityWhenUnblockUserBreaksBusinessRule() throws Exception {
        doThrow(new BusinessRuleException("Cannot self unblock"))
                .when(userService).unblockUser(eq(1L), any(Authentication.class));

        mockMvc.perform(post("/users/{id}/unblock", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Cannot self unblock"))
                .andExpect(jsonPath("$.path").value("/users/1/unblock"));

        verify(userService).unblockUser(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToDeleteUser() throws Exception {
        mockMvc.perform(post("/users/{id}/delete", 1L))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(eq(1L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeDeleteUser() throws Exception {
        mockMvc.perform(post("/users/{id}/delete", 1L))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any(), any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianDeleteUser() throws Exception {
        mockMvc.perform(post("/users/{id}/delete", 1L))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnNotFoundWhenDeletingUnknownUser() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).deleteUser(eq(99L), any(Authentication.class));

        mockMvc.perform(post("/users/{id}/delete", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.path").value("/users/99/delete"));

        verify(userService).deleteUser(eq(99L), any(Authentication.class));
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnUnprocessableEntityWhenDeleteUserBreaksBusinessRule() throws Exception {
        doThrow(new BusinessRuleException("Cannot self delete"))
                .when(userService).deleteUser(eq(1L), any(Authentication.class));

        mockMvc.perform(post("/users/{id}/delete", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Cannot self delete"))
                .andExpect(jsonPath("$.path").value("/users/1/delete"));

        verify(userService).deleteUser(eq(1L), any(Authentication.class));
    }

    private UserRequestDTO userRequest() {
        return new UserRequestDTO(
                "New User",
                "new.user@supportflow.test",
                "raw-password",
                UserRole.EMPLOYEE
        );
    }

    private UserResponseDTO userResponse(Long id, String name, String email, String role, String status) {
        return new UserResponseDTO(
                id,
                name,
                email,
                role,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
