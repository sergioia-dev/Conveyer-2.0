package conveyer.backend.business.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import conveyer.backend.persistance.model.Project;
import conveyer.backend.persistance.model.User;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

  boolean existsByNameAndUser(String name, User user);

  boolean existsByNameAndUserAndIdNot(String name, User user, UUID id);

  Optional<Project> findByIdAndUser(UUID id, User user);
}
