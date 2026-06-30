package conveyer.backend.view.controller;

import java.text.ParseException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import conveyer.backend.DTO.ApiKeyResponseDTO;
import conveyer.backend.DTO.CreateApiKeyDTO;
import conveyer.backend.DTO.CreateProjectDTO;
import conveyer.backend.DTO.EditProjectDTO;
import conveyer.backend.DTO.HTTPResponseDTO;
import conveyer.backend.business.repository.ProjectRepository;
import conveyer.backend.business.repository.UserRepository;
import conveyer.backend.business.service.ProjectManagementService;
import conveyer.backend.configuration.service.ApiKeyService;
import conveyer.backend.configuration.service.JwtService;
import conveyer.backend.persistance.model.Project;
import conveyer.backend.persistance.model.User;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

  private final ProjectManagementService projectManagementService;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final  ApiKeyService apiKeyService;

  public ProjectController(ProjectManagementService projectManagementService, ProjectRepository projectRepository,
      UserRepository userRepository,
      JwtService jwtService, ApiKeyService apiKeyService) {
    this.projectManagementService = projectManagementService;
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.apiKeyService = apiKeyService;
  }

  @PostMapping(path = "/create", version = "1")
  public ResponseEntity<?> createProject(@RequestBody CreateProjectDTO dto,
      @CookieValue(name = "accessToken", required = false) String accessToken) {

    if (dto.name() == null || dto.name().isEmpty()) {
      return ResponseEntity.badRequest()
          .body(new HTTPResponseDTO(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Missing parameters"));
    }

    if (accessToken == null || !jwtService.validateToken(accessToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Not authenticated"));
    }

    String email;
    try {
      email = jwtService.extractSubject(accessToken);
    } catch (ParseException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Invalid token"));
    }

    User user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "User not found"));
    }

    Project project = projectManagementService.createProject(dto, user);
    if (project == null) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(new HTTPResponseDTO(HttpStatus.CONFLICT.value(), "Conflict", "A project with this name already exists"));
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(project);
  }

  @PostMapping(path = "/edit", version = "1")
  public ResponseEntity<?> editProject(@RequestBody EditProjectDTO dto,
      @CookieValue(name = "accessToken", required = false) String accessToken) {

    if (dto.name() == null || dto.name().isEmpty()) {
      return ResponseEntity.badRequest()
          .body(new HTTPResponseDTO(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Missing parameters"));
    }

    if (accessToken == null || !jwtService.validateToken(accessToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Not authenticated"));
    }

    String email;
    try {
      email = jwtService.extractSubject(accessToken);
    } catch (ParseException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Invalid token"));
    }

    User user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "User not found"));
    }

    if (projectRepository.findByIdAndUser(dto.id(), user).isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new HTTPResponseDTO(HttpStatus.NOT_FOUND.value(), "Not Found", "Project not found"));
    }

    Project project = projectManagementService.editProject(dto, user);
    if (project == null) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(new HTTPResponseDTO(HttpStatus.CONFLICT.value(), "Conflict", "A project with this name already exists"));
    }

    return ResponseEntity.ok(project);
  }

  @PostMapping(path = "/{projectId}/apikey", version = "1")
  public ResponseEntity<?> generateApiKey(@PathVariable UUID projectId,
      @RequestBody CreateApiKeyDTO dto,
      @CookieValue(name = "accessToken", required = false) String accessToken) {

    if (dto.expiresInSeconds() != null && dto.expiresInSeconds() < 0) {
      return ResponseEntity.badRequest()
          .body(new HTTPResponseDTO(HttpStatus.BAD_REQUEST.value(), "Bad Request",
              "expiresInSeconds must be non-negative"));
    }

    if (accessToken == null || !jwtService.validateToken(accessToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Not authenticated"));
    }

    String email;
    try {
      email = jwtService.extractSubject(accessToken);
    } catch (ParseException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Invalid token"));
    }

    User user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "User not found"));
    }

    if (projectRepository.findByIdAndUser(projectId, user).isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new HTTPResponseDTO(HttpStatus.NOT_FOUND.value(), "Not Found", "Project not found"));
    }

    String apiKey = apiKeyService.generateApiKey(user.getId(), projectId, dto.expiresInSeconds());
    return ResponseEntity.ok(new ApiKeyResponseDTO(apiKey, dto.expiresInSeconds()));
  }
}
