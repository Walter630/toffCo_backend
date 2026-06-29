package com.site.toffCo.module.user.repository;

import com.site.toffCo.module.user.entity.Role;
import com.site.toffCo.module.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean findByRole(Role role);
}
