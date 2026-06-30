package conveyer.backend.DTO;

import java.time.LocalDateTime;

public class HTTPResponseDTO {
  private LocalDateTime timestamp = LocalDateTime.now();
  private int status;
  private String error; // HTTP error name (e.g., "Bad Request")
  private String message; // Human-readable description

  public HTTPResponseDTO() {
  }

  public HTTPResponseDTO(int status, String error, String message) {
    this.status = status;
    this.error = error;
    this.message = message;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  // constructors, getters
}
