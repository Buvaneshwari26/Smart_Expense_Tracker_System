package com.expense.crudtest.repository;

import com.expense.crudtest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}