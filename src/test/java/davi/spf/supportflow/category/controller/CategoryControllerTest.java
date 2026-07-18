package davi.spf.supportflow.category.controller;

import davi.spf.supportflow.auth.security.CustomJwtAuthenticationConverter;
import davi.spf.supportflow.category.dto.CategoryRequestDTO;
import davi.spf.supportflow.category.dto.CategoryResponseDTO;
import davi.spf.supportflow.category.dto.UpdateCategoryRequestDTO;
import davi.spf.supportflow.category.service.CategoryService;
import davi.spf.supportflow.common.config.ProjectSecurityConfig;
import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceAlreadyExistsException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({ProjectSecurityConfig.class, CustomJwtAuthenticationConverter.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToListCategories() throws Exception {
        Page<CategoryResponseDTO> response = new PageImpl<>(List.of(categoryResponse()));

        when(categoryService.listCategories(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Hardware"))
                .andExpect(jsonPath("$.content[0].description").value("Hardware support"))
                .andExpect(jsonPath("$.content[0].active").value(true));

        verify(categoryService).listCategories(any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldAllowTechnicianToListCategories() throws Exception {
        Page<CategoryResponseDTO> response = new PageImpl<>(List.of(categoryResponse()));

        when(categoryService.listCategories(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Hardware"));

        verify(categoryService).listCategories(any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldAllowEmployeeToListCategories() throws Exception {
        Page<CategoryResponseDTO> response = new PageImpl<>(List.of(categoryResponse()));

        when(categoryService.listCategories(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Hardware"));

        verify(categoryService).listCategories(any(Pageable.class));
    }

    @Test
    void shouldRejectUnauthenticatedListCategories() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized());

        verify(categoryService, never()).listCategories(any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToFindCategoryById() throws Exception {
        CategoryResponseDTO response = categoryResponse();

        when(categoryService.findCategoryById(1L)).thenReturn(response);

        mockMvc.perform(get("/categories/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Hardware"))
                .andExpect(jsonPath("$.description").value("Hardware support"))
                .andExpect(jsonPath("$.active").value(true));

        verify(categoryService).findCategoryById(1L);
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeFindCategoryById() throws Exception {
        mockMvc.perform(get("/categories/{id}", 1L))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).findCategoryById(any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianFindCategoryById() throws Exception {
        mockMvc.perform(get("/categories/{id}", 1L))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).findCategoryById(any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnNotFoundWhenFindingUnknownCategoryById() throws Exception {
        when(categoryService.findCategoryById(99L)).thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(get("/categories/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value("Category not found"))
                .andExpect(jsonPath("$.path").value("/categories/99"));

        verify(categoryService).findCategoryById(99L);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToCreateCategory() throws Exception {
        CategoryRequestDTO request = categoryRequest();
        CategoryResponseDTO response = categoryResponse();

        when(categoryService.createCategory(request)).thenReturn(response);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Hardware"))
                .andExpect(jsonPath("$.description").value("Hardware support"))
                .andExpect(jsonPath("$.active").value(true));

        verify(categoryService).createCategory(request);
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeCreateCategory() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest())))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianCreateCategory() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest())))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    void shouldRejectUnauthenticatedCreateCategory() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest())))
                .andExpect(status().isUnauthorized());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnConflictWhenCreatingExistingCategory() throws Exception {
        CategoryRequestDTO request = categoryRequest();

        when(categoryService.createCategory(request))
                .thenThrow(new ResourceAlreadyExistsException("Category already exists"));

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Resource Conflict"))
                .andExpect(jsonPath("$.message").value("Category already exists"))
                .andExpect(jsonPath("$.path").value("/categories"));

        verify(categoryService).createCategory(request);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToUpdateCategory() throws Exception {
        UpdateCategoryRequestDTO request = updateRequest();
        CategoryResponseDTO response = categoryResponse();

        when(categoryService.updateCategory(1L, request)).thenReturn(response);

        mockMvc.perform(put("/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Hardware"))
                .andExpect(jsonPath("$.description").value("Hardware support"))
                .andExpect(jsonPath("$.active").value(true));

        verify(categoryService).updateCategory(1L, request);
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeUpdateCategory() throws Exception {
        mockMvc.perform(put("/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).updateCategory(any(), any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianUpdateCategory() throws Exception {
        mockMvc.perform(put("/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).updateCategory(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnNotFoundWhenUpdatingUnknownCategory() throws Exception {
        UpdateCategoryRequestDTO request = updateRequest();

        when(categoryService.updateCategory(99L, request))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(put("/categories/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value("Category not found"))
                .andExpect(jsonPath("$.path").value("/categories/99"));

        verify(categoryService).updateCategory(99L, request);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnUnprocessableContentWhenUpdatingCategoryBreaksBusinessRule() throws Exception {
        UpdateCategoryRequestDTO request = updateRequest();

        when(categoryService.updateCategory(1L, request))
                .thenThrow(new BusinessRuleException("Category name already taken"));

        mockMvc.perform(put("/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Category name already taken"))
                .andExpect(jsonPath("$.path").value("/categories/1"));

        verify(categoryService).updateCategory(1L, request);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToActivateCategory() throws Exception {
        mockMvc.perform(patch("/categories/{id}/activate", 1L))
                .andExpect(status().isNoContent());

        verify(categoryService).activateCategory(1L);
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeActivateCategory() throws Exception {
        mockMvc.perform(patch("/categories/{id}/activate", 1L))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).activateCategory(any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianActivateCategory() throws Exception {
        mockMvc.perform(patch("/categories/{id}/activate", 1L))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).activateCategory(any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnNotFoundWhenActivatingUnknownCategory() throws Exception {
        doThrow(new ResourceNotFoundException("Category not found")).when(categoryService).activateCategory(99L);

        mockMvc.perform(patch("/categories/{id}/activate", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value("Category not found"))
                .andExpect(jsonPath("$.path").value("/categories/99/activate"));

        verify(categoryService).activateCategory(99L);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnUnprocessableContentWhenActivatingCategoryBreaksBusinessRule() throws Exception {
        doThrow(new BusinessRuleException("Category already active")).when(categoryService).activateCategory(1L);

        mockMvc.perform(patch("/categories/{id}/activate", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Category already active"))
                .andExpect(jsonPath("$.path").value("/categories/1/activate"));

        verify(categoryService).activateCategory(1L);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToDeactivateCategory() throws Exception {
        mockMvc.perform(patch("/categories/{id}/deactivate", 1L))
                .andExpect(status().isNoContent());

        verify(categoryService).deactivateCategory(1L);
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeDeactivateCategory() throws Exception {
        mockMvc.perform(patch("/categories/{id}/deactivate", 1L))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).deactivateCategory(any());
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldRejectTechnicianDeactivateCategory() throws Exception {
        mockMvc.perform(patch("/categories/{id}/deactivate", 1L))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).deactivateCategory(any());
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnNotFoundWhenDeactivatingUnknownCategory() throws Exception {
        doThrow(new ResourceNotFoundException("Category not found")).when(categoryService).deactivateCategory(99L);

        mockMvc.perform(patch("/categories/{id}/deactivate", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not found"))
                .andExpect(jsonPath("$.message").value("Category not found"))
                .andExpect(jsonPath("$.path").value("/categories/99/deactivate"));

        verify(categoryService).deactivateCategory(99L);
    }

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldReturnUnprocessableContentWhenDeactivatingCategoryBreaksBusinessRule() throws Exception {
        doThrow(new BusinessRuleException("Category is not active")).when(categoryService).deactivateCategory(1L);

        mockMvc.perform(patch("/categories/{id}/deactivate", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Category is not active"))
                .andExpect(jsonPath("$.path").value("/categories/1/deactivate"));

        verify(categoryService).deactivateCategory(1L);
    }

    private CategoryRequestDTO categoryRequest() {
        return new CategoryRequestDTO("Hardware", "Hardware support");
    }

    private UpdateCategoryRequestDTO updateRequest() {
        return new UpdateCategoryRequestDTO("Hardware", "Hardware support");
    }

    private CategoryResponseDTO categoryResponse() {
        return new CategoryResponseDTO(
                1L,
                "Hardware",
                "Hardware support",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
