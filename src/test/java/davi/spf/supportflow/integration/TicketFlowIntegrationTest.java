package davi.spf.supportflow.integration;

import com.jayway.jsonpath.JsonPath;
import davi.spf.supportflow.auth.dto.LoginRequest;
import davi.spf.supportflow.category.entity.Category;
import davi.spf.supportflow.category.repository.CategoryRepository;
import davi.spf.supportflow.comment.dto.TicketCommentRequestDTO;
import davi.spf.supportflow.comment.repository.TicketCommentRepository;
import davi.spf.supportflow.history.repository.TicketHistoryRepository;
import davi.spf.supportflow.rating.dto.TicketRatingRequestDTO;
import davi.spf.supportflow.rating.repository.TicketRatingRepository;
import davi.spf.supportflow.ticket.dto.TicketRequestDTO;
import davi.spf.supportflow.ticket.enums.TicketPriority;
import davi.spf.supportflow.ticket.repository.TicketRepository;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.enums.UserStatus;
import davi.spf.supportflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TicketFlowIntegrationTest {

    private static final String PASSWORD = "password123";

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4")
            .withDatabaseName("support_flow_test")
            .withUsername("support_flow_user")
            .withPassword("support_flow_password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketHistoryRepository ticketHistoryRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Autowired
    private TicketRatingRepository ticketRatingRepository;

    private User admin;
    private User employee;
    private User technician;
    private Category category;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @BeforeEach
    void setUp() {
        cleanDatabase();

        admin = userRepository.save(user("Admin", "admin.integration@supportflow.test", UserRole.ADMIN));
        employee = userRepository.save(user("Employee", "employee.integration@supportflow.test", UserRole.EMPLOYEE));
        technician = userRepository.save(user("Technician", "technician.integration@supportflow.test", UserRole.TECHNICIAN));
        category = categoryRepository.save(category());
    }

    @Test
    void shouldCompleteMainTicketFlowWithRealJwtSecurityPersistenceHistoryAndDashboard() throws Exception {
        String adminToken = login(admin.getEmail());
        String employeeToken = login(employee.getEmail());
        String technicianToken = login(technician.getEmail());

        Long ticketId = createTicket(employeeToken);

        mockMvc.perform(patch("/tickets/{ticketId}/claim", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(technicianToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedTo.id").value(technician.getId()))
                .andExpect(jsonPath("$.assignedTo.role").value("TECHNICIAN"));

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(technicianToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketCommentRequestDTO("Investigating the issue"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.authorId").value(technician.getId()))
                .andExpect(jsonPath("$.authorRole").value("TECHNICIAN"))
                .andExpect(jsonPath("$.message").value("Investigating the issue"));

        mockMvc.perform(patch("/tickets/{ticketId}/resolve", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(technicianToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(patch("/tickets/{ticketId}/close", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(employeeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(post("/tickets/{ticketId}/rating", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(employeeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketRatingRequestDTO(5, "Great support"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.ratedById").value(employee.getId()))
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.comment").value("Great support"));

        mockMvc.perform(get("/tickets/{ticketId}/history", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].action", containsInAnyOrder(
                        "CREATED",
                        "CLAIMED",
                        "RESOLVED",
                        "CLOSED"
                )));

        mockMvc.perform(get("/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.closedTickets").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldEnforceSecurityRestrictionsInTicketFlow() throws Exception {
        String employeeToken = login(employee.getEmail());
        String technicianToken = login(technician.getEmail());
        Long ticketId = createTicket(employeeToken);

        mockMvc.perform(patch("/tickets/{ticketId}/resolve", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(employeeToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/tickets/{ticketId}/rating", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(technicianToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketRatingRequestDTO(5, "Not allowed"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/tickets"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employeeToken)))
                .andExpect(status().isForbidden());
    }

    private Long createTicket(String employeeToken) throws Exception {
        TicketRequestDTO request = new TicketRequestDTO(
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH,
                category.getId()
        );

        MvcResult result = mockMvc.perform(post("/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employeeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdBy.id").value(employee.getId()))
                .andExpect(jsonPath("$.category.id").value(category.getId()))
                .andReturn();

        Number ticketId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return ticketId.longValue();
    }

    private String login(String email) throws Exception {
        LoginRequest request = new LoginRequest(email, PASSWORD);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private User user(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Category category() {
        Category category = new Category();
        category.setName("Hardware");
        category.setDescription("Hardware support");
        category.setActive(true);
        return category;
    }

    private void cleanDatabase() {
        ticketRatingRepository.deleteAllInBatch();
        ticketCommentRepository.deleteAllInBatch();
        ticketHistoryRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }
}
