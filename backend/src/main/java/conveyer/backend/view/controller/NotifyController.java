package conveyer.backend.view.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import conveyer.backend.DTO.HTTPResponseDTO;
import conveyer.backend.DTO.VerifyNotificationDTO;
import conveyer.backend.DTO.VerifyNotificationResponseDTO;
import conveyer.backend.business.service.InvalidApiKeyException;
import conveyer.backend.business.service.NotificationFailedException;
import conveyer.backend.business.service.NotifyService;
import conveyer.backend.business.service.ProjectNotFoundException;

@RestController
@RequestMapping("/api/notify")
public class NotifyController {

  private final NotifyService notifyService;

  public NotifyController(NotifyService notifyService) {
    this.notifyService = notifyService;
  }

  @PostMapping(path = "/verify", version = "1")
  public ResponseEntity<?> verify(@RequestHeader("Authorization") String authHeader,
      @RequestBody VerifyNotificationDTO dto) {
    try {
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "API key is required"));
      }
      String apiKey = authHeader.substring(7);
      VerifyNotificationResponseDTO result = notifyService.sendNotification(
          apiKey, dto.to(), dto.code(), dto.notificationType());
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(new HTTPResponseDTO(HttpStatus.BAD_REQUEST.value(), "Bad Request", e.getMessage()));
    } catch (InvalidApiKeyException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HTTPResponseDTO(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", e.getMessage()));
    } catch (ProjectNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new HTTPResponseDTO(HttpStatus.NOT_FOUND.value(), "Not Found", e.getMessage()));
    } catch (NotificationFailedException e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(new HTTPResponseDTO(HttpStatus.BAD_GATEWAY.value(), "Bad Gateway", e.getMessage()));
    }
  }
}
