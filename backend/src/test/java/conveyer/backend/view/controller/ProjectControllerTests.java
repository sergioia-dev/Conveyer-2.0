package conveyer.backend.view.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import java.text.ParseException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import conveyer.backend.DTO.CreateApiKeyDTO;
import conveyer.backend.DTO.CreateProjectDTO;
import conveyer.backend.DTO.EditProjectDTO;
import conveyer.backend.business.repository.ProjectRepository;
import conveyer.backend.business.repository.UserRepository;
import conveyer.backend.business.service.ProjectManagementService;
import conveyer.backend.configuration.service.ApiKeyService;
import conveyer.backend.configuration.service.JwtService;
import conveyer.backend.configuration.service.RateLimitingService;
import conveyer.backend.persistance.model.Project;
import conveyer.backend.persistance.model.User;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProjectController.class)
class ProjectControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ProjectManagementService projectManagementService;

  @MockitoBean
  private ProjectRepository projectRepository;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private RateLimitingService rateLimitingService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @MockitoBean
  private ApiKeyService apiKeyService;

  private final String BEARER_TOKEN = "bearer.token";
  private final String USER_EMAIL = "user@example.com";

  private UserDetails userDetails;

  @BeforeEach
  void setUp() throws ParseException {
    userDetails = org.springframework.security.core.userdetails.User.withUsername(USER_EMAIL)
        .password("pass").authorities("ROLE_USER").build();

    when(jwtService.validateToken(BEARER_TOKEN)).thenReturn(true);
    when(jwtService.extractSubject(BEARER_TOKEN)).thenReturn(USER_EMAIL);
    when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(userDetails);

    ConsumptionProbe probe = mock(ConsumptionProbe.class);
    when(probe.isConsumed()).thenReturn(true);
    when(probe.getRemainingTokens()).thenReturn(9L);

    Bucket mockBucket = mock(Bucket.class);
    when(mockBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

    when(rateLimitingService.resolveBucket(anyString())).thenReturn(mockBucket);
  }

  @Test
  void createProject_missingName_returns400() throws Exception {
    CreateProjectDTO dto = new CreateProjectDTO(null, "example.com", "A test project");

    mockMvc.perform(post("/api/project/create")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void createProject_emptyName_returns400() throws Exception {
    CreateProjectDTO dto = new CreateProjectDTO("", "example.com", "A test project");

    mockMvc.perform(post("/api/project/create")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void createProject_noCookie_returns401() throws Exception {
    CreateProjectDTO dto = new CreateProjectDTO("My Project", "example.com", "A test project");

    mockMvc.perform(post("/api/project/create")
        .header("X-API-Version", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void createProject_invalidToken_returns401() throws Exception {
    when(jwtService.validateToken("bad.token")).thenReturn(false);

    CreateProjectDTO dto = new CreateProjectDTO("My Project", "example.com", "A test project");

    mockMvc.perform(post("/api/project/create")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", "bad.token"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void createProject_parseException_returns401() throws Exception {
    when(jwtService.validateToken("parse.token")).thenReturn(true);
    when(jwtService.extractSubject("parse.token")).thenThrow(new ParseException("Bad token", 0));

    CreateProjectDTO dto = new CreateProjectDTO("My Project", "example.com", "A test project");

    mockMvc.perform(post("/api/project/create")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", "parse.token"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void createProject_userNotFound_returns401() throws Exception {
    String unknownEmail = "unknown@example.com";
    when(jwtService.extractSubject(BEARER_TOKEN)).thenReturn(unknownEmail);
    when(userDetailsService.loadUserByUsername(unknownEmail)).thenReturn(userDetails);
    when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

    CreateProjectDTO dto = new CreateProjectDTO("My Project", "example.com", "A test project");

    mockMvc.perform(post("/api/project/create")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void createProject_duplicateName_returns409() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(USER_EMAIL);

    when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
    when(projectManagementService.createProject(any(CreateProjectDTO.class), any(User.class))).thenReturn(null);

    CreateProjectDTO dto = new CreateProjectDTO("Existing Project", "example.com", "A test project");

    mockMvc.perform(post("/api/project/create")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("Conflict"));
  }

  @Test
  void createProject_success_returns201() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(USER_EMAIL);

    Project project = new Project();
    project.setId(UUID.randomUUID());
    project.setName("My Project");
    project.setDomain("example.com");
    project.setDescription("A test project");
    project.setUser(user);

    when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
    when(projectManagementService.createProject(any(CreateProjectDTO.class), any(User.class))).thenReturn(project);

    CreateProjectDTO dto = new CreateProjectDTO("My Project", "example.com", "A test project");

    mockMvc.perform(post("/api/project/create")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("My Project"))
        .andExpect(jsonPath("$.domain").value("example.com"))
        .andExpect(jsonPath("$.description").value("A test project"));
  }

  @Test
  void editProject_missingName_returns400() throws Exception {
    EditProjectDTO dto = new EditProjectDTO(UUID.randomUUID(), null, "example.com", "A test project");

    mockMvc.perform(post("/api/project/edit")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void editProject_emptyName_returns400() throws Exception {
    EditProjectDTO dto = new EditProjectDTO(UUID.randomUUID(), "", "example.com", "A test project");

    mockMvc.perform(post("/api/project/edit")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void editProject_noCookie_returns401() throws Exception {
    EditProjectDTO dto = new EditProjectDTO(UUID.randomUUID(), "Updated Name", "example.com", "A test project");

    mockMvc.perform(post("/api/project/edit")
        .header("X-API-Version", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void editProject_invalidToken_returns401() throws Exception {
    when(jwtService.validateToken("bad.token")).thenReturn(false);

    EditProjectDTO dto = new EditProjectDTO(UUID.randomUUID(), "Updated Name", "example.com", "A test project");

    mockMvc.perform(post("/api/project/edit")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", "bad.token"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void editProject_parseException_returns401() throws Exception {
    when(jwtService.validateToken("parse.token")).thenReturn(true);
    when(jwtService.extractSubject("parse.token")).thenThrow(new ParseException("Bad token", 0));

    EditProjectDTO dto = new EditProjectDTO(UUID.randomUUID(), "Updated Name", "example.com", "A test project");

    mockMvc.perform(post("/api/project/edit")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", "parse.token"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void editProject_userNotFound_returns401() throws Exception {
    String unknownEmail = "unknown@example.com";
    when(jwtService.extractSubject(BEARER_TOKEN)).thenReturn(unknownEmail);
    when(userDetailsService.loadUserByUsername(unknownEmail)).thenReturn(userDetails);
    when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

    EditProjectDTO dto = new EditProjectDTO(UUID.randomUUID(), "Updated Name", "example.com", "A test project");

    mockMvc.perform(post("/api/project/edit")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void editProject_projectNotFound_returns404() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(USER_EMAIL);

    UUID projectId = UUID.randomUUID();

    when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
    when(projectRepository.findByIdAndUser(projectId, user)).thenReturn(Optional.empty());

    EditProjectDTO dto = new EditProjectDTO(projectId, "Updated Name", "example.com", "A test project");

    mockMvc.perform(post("/api/project/edit")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Not Found"));
  }

  @Test
  void editProject_duplicateName_returns409() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(USER_EMAIL);

    UUID projectId = UUID.randomUUID();
    Project existingProject = new Project();
    existingProject.setId(projectId);
    existingProject.setName("Original Name");
    existingProject.setUser(user);

    when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
    when(projectRepository.findByIdAndUser(projectId, user)).thenReturn(Optional.of(existingProject));
    when(projectManagementService.editProject(any(EditProjectDTO.class), any(User.class))).thenReturn(null);

    EditProjectDTO dto = new EditProjectDTO(projectId, "Duplicate Name", "example.com", "A test project");

    mockMvc.perform(post("/api/project/edit")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("Conflict"));
  }

  @Test
  void editProject_success_returns200() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(USER_EMAIL);

    UUID projectId = UUID.randomUUID();
    Project existingProject = new Project();
    existingProject.setId(projectId);
    existingProject.setName("Original Name");
    existingProject.setDomain("original.com");
    existingProject.setDescription("Original description");
    existingProject.setUser(user);

    Project updatedProject = new Project();
    updatedProject.setId(projectId);
    updatedProject.setName("Updated Name");
    updatedProject.setDomain("updated.com");
    updatedProject.setDescription("Updated description");
    updatedProject.setUser(user);

    when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
    when(projectRepository.findByIdAndUser(projectId, user)).thenReturn(Optional.of(existingProject));
    when(projectManagementService.editProject(any(EditProjectDTO.class), any(User.class))).thenReturn(updatedProject);

    EditProjectDTO dto = new EditProjectDTO(projectId, "Updated Name", "updated.com", "Updated description");

    mockMvc.perform(post("/api/project/edit")
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.domain").value("updated.com"))
        .andExpect(jsonPath("$.description").value("Updated description"));
  }

  @Test
  void generateApiKey_noCookie_returns401() throws Exception {
    CreateApiKeyDTO dto = new CreateApiKeyDTO(null);

    mockMvc.perform(post("/api/project/{projectId}/apikey", UUID.randomUUID())
        .header("X-API-Version", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void generateApiKey_invalidToken_returns401() throws Exception {
    when(jwtService.validateToken("bad.token")).thenReturn(false);

    CreateApiKeyDTO dto = new CreateApiKeyDTO(null);

    mockMvc.perform(post("/api/project/{projectId}/apikey", UUID.randomUUID())
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", "bad.token"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Unauthorized"));
  }

  @Test
  void generateApiKey_negativeExpiresIn_returns400() throws Exception {
    CreateApiKeyDTO dto = new CreateApiKeyDTO(-1L);

    mockMvc.perform(post("/api/project/{projectId}/apikey", UUID.randomUUID())
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void generateApiKey_projectNotFound_returns404() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(USER_EMAIL);

    UUID projectId = UUID.randomUUID();

    when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
    when(projectRepository.findByIdAndUser(projectId, user)).thenReturn(Optional.empty());

    CreateApiKeyDTO dto = new CreateApiKeyDTO(null);

    mockMvc.perform(post("/api/project/{projectId}/apikey", projectId)
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Not Found"));
  }

  @Test
  void generateApiKey_success_returns200() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(USER_EMAIL);

    UUID projectId = UUID.randomUUID();
    Project project = new Project();
    project.setId(projectId);
    project.setName("My Project");
    project.setUser(user);

    when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
    when(projectRepository.findByIdAndUser(projectId, user)).thenReturn(Optional.of(project));
    when(apiKeyService.generateApiKey(user.getId(), projectId, null)).thenReturn("sk-abc123def456");

    CreateApiKeyDTO dto = new CreateApiKeyDTO(null);

    mockMvc.perform(post("/api/project/{projectId}/apikey", projectId)
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apiKey").value("sk-abc123def456"))
        .andExpect(jsonPath("$.expiresInSeconds").doesNotExist());
  }

  @Test
  void generateApiKey_withExpiration_returns200() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(USER_EMAIL);

    UUID projectId = UUID.randomUUID();
    Project project = new Project();
    project.setId(projectId);
    project.setName("My Project");
    project.setUser(user);

    when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
    when(projectRepository.findByIdAndUser(projectId, user)).thenReturn(Optional.of(project));
    when(apiKeyService.generateApiKey(user.getId(), projectId, 3600L)).thenReturn("sk-xyz789");

    CreateApiKeyDTO dto = new CreateApiKeyDTO(3600L);

    mockMvc.perform(post("/api/project/{projectId}/apikey", projectId)
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apiKey").value("sk-xyz789"))
        .andExpect(jsonPath("$.expiresInSeconds").value(3600));
  }

  @Test
  void generateApiKey_zeroExpiresIn_returns200() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(USER_EMAIL);

    UUID projectId = UUID.randomUUID();
    Project project = new Project();
    project.setId(projectId);
    project.setName("My Project");
    project.setUser(user);

    when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
    when(projectRepository.findByIdAndUser(projectId, user)).thenReturn(Optional.of(project));
    when(apiKeyService.generateApiKey(user.getId(), projectId, 0L)).thenReturn("sk-zero-ttl");

    CreateApiKeyDTO dto = new CreateApiKeyDTO(0L);

    mockMvc.perform(post("/api/project/{projectId}/apikey", projectId)
        .header("X-API-Version", "1")
        .cookie(new jakarta.servlet.http.Cookie("accessToken", BEARER_TOKEN))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apiKey").value("sk-zero-ttl"))
        .andExpect(jsonPath("$.expiresInSeconds").value(0));
  }

  @TestConfiguration
  @EnableWebSecurity
  static class TestSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .csrf(csrf -> csrf.disable())
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
      return http.build();
    }
  }
}
