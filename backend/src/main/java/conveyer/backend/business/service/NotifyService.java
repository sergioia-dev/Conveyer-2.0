package conveyer.backend.business.service;

import java.security.SecureRandom;
import java.text.ParseException;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;

import conveyer.backend.DTO.VerifyNotificationResponseDTO;
import conveyer.backend.business.repository.ProjectRepository;
import conveyer.backend.business.service.implementation.EmailService;
import conveyer.backend.business.service.implementation.SMSService;
import conveyer.backend.configuration.service.ApiKeyService;
import conveyer.backend.persistance.model.Project;

@Service
public class NotifyService {

  private final ApiKeyService apiKeyService;
  private final ProjectRepository projectRepository;
  private final EmailService emailService;
  private final SMSService smsService;

  public NotifyService(ApiKeyService apiKeyService, ProjectRepository projectRepository,
      EmailService emailService, SMSService smsService) {
    this.apiKeyService = apiKeyService;
    this.projectRepository = projectRepository;
    this.emailService = emailService;
    this.smsService = smsService;
  }

  public VerifyNotificationResponseDTO sendNotification(
      String apiKey, String to, String code, NotificationType notificationType) {

    if (apiKey == null || apiKey.isBlank()) {
      throw new InvalidApiKeyException("API key is required");
    }

    if (to == null || to.isBlank()) {
      throw new IllegalArgumentException("to is required");
    }

    if (notificationType == null) {
      throw new IllegalArgumentException("notificationType is required");
    }

    if (code == null || code.isBlank()) {
      code = generateCode();
    }

    UUID projectId;
    try {
      String projectIdStr = apiKeyService.extractProjectId(apiKey);
      projectId = UUID.fromString(projectIdStr);
    } catch (ParseException | JOSEException | IllegalArgumentException e) {
      throw new InvalidApiKeyException("Invalid API key");
    }

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project == null) {
      throw new ProjectNotFoundException("Project not found");
    }

    boolean sent;
    switch (notificationType) {
      case EMAIL -> sent = emailService.sendVerificationCode(to, code, project.getName());
      case SMS -> sent = smsService.sendVerificationCode(to, code, project.getName());
      default -> throw new IllegalArgumentException("Unsupported notification type: " + notificationType);
    }

    if (!sent) {
      throw new NotificationFailedException("Failed to send notification");
    }

    return new VerifyNotificationResponseDTO(code, true);
  }

  private String generateCode() {
    SecureRandom random = new SecureRandom();
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }
}
