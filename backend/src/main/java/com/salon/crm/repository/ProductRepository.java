package com.salon.crm.repository;

import com.salon.crm.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByActiveTrue();
    List<Product> findByStockQtyLessThanEqual(int threshold);
}
