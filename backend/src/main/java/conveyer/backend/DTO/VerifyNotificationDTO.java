package conveyer.backend.DTO;

import conveyer.backend.business.service.NotificationType;

public record VerifyNotificationDTO(String to, String code, NotificationType notificationType) {
}
