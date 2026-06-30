package conveyer.backend.business.service;

public class NotificationFailedException extends RuntimeException {

  public NotificationFailedException(String message) {
    super(message);
  }
}
