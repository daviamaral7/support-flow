package davi.spf.supportflow.dashboard.controller;

import davi.spf.supportflow.auth.security.CustomJwtAuthenticationConverter;
import davi.spf.supportflow.common.config.ProjectSecurityConfig;
import davi.spf.supportflow.dashboard.dto.DashboardSummaryDTO;
import davi.spf.supportflow.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({ProjectSecurityConfig.class, CustomJwtAuthenticationConverter.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    @WithMockUser(username = "admin@supportflow.test", roles = "ADMIN")
    void shouldAllowAdminToGetDashboardSummary() throws Exception {
        DashboardSummaryDTO response = dashboardSummary();

        when(dashboardService.getSummary()).thenReturn(response);

        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(10L))
                .andExpect(jsonPath("$.openTickets").value(2L))
                .andExpect(jsonPath("$.inProgressTickets").value(3L))
                .andExpect(jsonPath("$.resolvedTickets").value(1L))
                .andExpect(jsonPath("$.closedTickets").value(2L))
                .andExpect(jsonPath("$.cancelledTickets").value(1L))
                .andExpect(jsonPath("$.criticalTickets").value(4L))
                .andExpect(jsonPath("$.unassignedTickets").value(5L));

        verify(dashboardService).getSummary();
    }

    @Test
    @WithMockUser(username = "technician@supportflow.test", roles = "TECHNICIAN")
    void shouldAllowTechnicianToGetDashboardSummary() throws Exception {
        DashboardSummaryDTO response = dashboardSummary();

        when(dashboardService.getSummary()).thenReturn(response);

        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(10L))
                .andExpect(jsonPath("$.openTickets").value(2L))
                .andExpect(jsonPath("$.inProgressTickets").value(3L))
                .andExpect(jsonPath("$.resolvedTickets").value(1L))
                .andExpect(jsonPath("$.closedTickets").value(2L))
                .andExpect(jsonPath("$.cancelledTickets").value(1L))
                .andExpect(jsonPath("$.criticalTickets").value(4L))
                .andExpect(jsonPath("$.unassignedTickets").value(5L));

        verify(dashboardService).getSummary();
    }

    @Test
    @WithMockUser(username = "employee@supportflow.test", roles = "EMPLOYEE")
    void shouldRejectEmployeeGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isForbidden());

        verify(dashboardService, never()).getSummary();
    }

    @Test
    void shouldRejectUnauthenticatedGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isUnauthorized());

        verify(dashboardService, never()).getSummary();
    }

    private DashboardSummaryDTO dashboardSummary() {
        return new DashboardSummaryDTO(
                10L,
                2L,
                3L,
                1L,
                2L,
                1L,
                4L,
                5L
        );
    }
}
