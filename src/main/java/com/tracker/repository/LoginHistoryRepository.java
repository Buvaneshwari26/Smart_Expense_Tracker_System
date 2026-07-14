package com.tracker.repository;

import com.tracker.model.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT h FROM LoginHistory h WHERE :userId IS NULL OR h.user.id = :userId")
    Page<LoginHistory> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId, Pageable pageable);

    List<LoginHistory> findTop10ByUserIdOrderByLoginAtDesc(Long userId);
    List<LoginHistory> findTop10ByOrderByLoginAtDesc();
}
