package com.salon.crm.repository;

import com.salon.crm.entity.Client;
import com.salon.crm.entity.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    boolean existsByPhone(String phone);

    Optional<Client> findByPhone(String phone);

    @Query("SELECT DISTINCT c FROM Client c LEFT JOIN c.tags t " +
            "WHERE (:search IS NULL OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "   OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:tag IS NULL OR t = :tag) " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<Client> search(@Param("search") String search,
                        @Param("tag") String tag,
                        @Param("status") ClientStatus status,
                        Pageable pageable);
}
