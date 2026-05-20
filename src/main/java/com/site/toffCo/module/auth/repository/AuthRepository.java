package com.site.toffCo.module.auth.repository;

import com.site.toffCo.module.auth.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuthRepository extends JpaRepository<Auth, UUID> {
}
