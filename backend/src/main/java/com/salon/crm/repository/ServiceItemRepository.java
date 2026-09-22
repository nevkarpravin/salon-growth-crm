package com.salon.crm.repository;

import com.salon.crm.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
    List<ServiceItem> findByActiveTrue();
    List<ServiceItem> findByIdIn(List<UUID> ids);
}
