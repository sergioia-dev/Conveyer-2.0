package conveyer.backend.persistance.model;

import java.util.UUID;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import conveyer.backend.DTO.enums.ProviderTypes;

@Entity
@Table(name = "users")
public class User {

  @Id
  @Column(name = "id", nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
  private UUID id = UUID.randomUUID();

  @Column(name = "username", nullable = false, length = 200)
  private String username = "";

  @Column(name = "email", nullable = false, unique = true, length = 320)
  private String email;

  @JsonIgnore
  @Column(name = "password", columnDefinition = "TEXT")
  private String password;

  @Enumerated(EnumType.STRING)
  private ProviderTypes provider = ProviderTypes.LOCAL;

  @Column(name = "created_at", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private LocalDateTime created_at = LocalDateTime.now();

  public User() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public LocalDateTime getCreated_at() {
    return created_at;
  }

  public void setCreated_at(LocalDateTime created_at) {
    this.created_at = created_at;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public ProviderTypes getProvider() {
    return provider;
  }

  public void setProvider(ProviderTypes provider) {
    this.provider = provider;
  }

}
