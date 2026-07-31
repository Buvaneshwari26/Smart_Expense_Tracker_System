package com.tracker.repository;

import com.tracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByUsernameAndIdNot(String username, Long id);

    long countByAccountActiveTrue();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.accountActive = false")
    long countByAccountActiveFalse();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    long countByRole(String role);

    long countByAccountLockedTrue();

    /**
     * Full-text search across fullName, email, username with optional filters
     * for role, accountActive, and accountLocked.
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:role IS NULL OR :role = '' OR u.role = :role) " +
           "AND (:active IS NULL OR u.accountActive = :active) " +
           "AND (:locked IS NULL OR u.accountLocked = :locked)")
    Page<User> searchUsers(@Param("search") String search,
                           @Param("role") String role,
                           @Param("active") Boolean active,
                           @Param("locked") Boolean locked,
                           Pageable pageable);
}
