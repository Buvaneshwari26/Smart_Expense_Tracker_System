package com.tracker.repository;

import com.tracker.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE (:userId IS NULL OR a.user.id = :userId) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:keyword IS NULL OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:startDate IS NULL OR a.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<ActivityLog> searchLogs(@Param("userId") Long userId,
                                 @Param("action") String action,
                                 @Param("keyword") String keyword,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate,
                                 Pageable pageable);
}
