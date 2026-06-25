package com.site.toffCo.module.whatzap.repository;

import com.site.toffCo.module.whatzap.entity.Whatzap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface WhatzapRepository extends JpaRepository<Whatzap, String> {
    Optional<Whatzap> findByWhatsappId(String whatsappId);
}
