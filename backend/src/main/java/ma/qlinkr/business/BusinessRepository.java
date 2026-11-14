package ma.qlinkr.business;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    boolean existsBySlug(String slug);
}
