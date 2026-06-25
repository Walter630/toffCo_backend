package com.site.toffCo.module.produto.repository;

import com.site.toffCo.module.produto.entity.Produto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, UUID>, JpaSpecificationExecutor<Produto> {
    Optional<Produto> findByDescription(@Param("descricao") String description);
    Optional<Produto> findByCategoria(@Param("categoria") String categoria);
    Optional<Produto> findByPrice(@Param("price") BigDecimal price);
    Page<Produto> findByCategoriaAndAtivoTrue(String categoria, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE) //trava o item do estoque
    @Query("SELECT p FROM Produto p WHERE p.id = :id")
    Optional<Produto> findByIdForUpdate(@Param("id") UUID id);

    UUID id(UUID id);
    boolean existsById(UUID id);
}
