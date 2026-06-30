package conveyer.backend.business.service;

public class InvalidApiKeyException extends RuntimeException {

  public InvalidApiKeyException(String message) {
    super(message);
  }
}
