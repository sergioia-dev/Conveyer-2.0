package conveyer.backend.business.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import conveyer.backend.DTO.enums.ProviderTypes;
import conveyer.backend.persistance.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailAndProvider(String email, ProviderTypes provider);

  boolean existsByEmail(String email);

}
