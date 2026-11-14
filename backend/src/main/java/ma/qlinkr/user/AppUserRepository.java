package ma.qlinkr.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Every lookup of a business member takes the business id as a parameter,
 * so a caller cannot load a member of another business by guessing its id.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<AppUser> findByBusinessIdOrderByIdAsc(long businessId);

    Optional<AppUser> findByIdAndBusinessId(long id, long businessId);
}
