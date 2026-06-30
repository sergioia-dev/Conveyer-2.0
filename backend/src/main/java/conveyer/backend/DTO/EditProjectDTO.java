package conveyer.backend.DTO;

import java.util.UUID;

public record EditProjectDTO(UUID id, String name, String domain, String description) {
}
