package com.salon.crm.service;

import com.salon.crm.dto.ProductRequest;
import com.salon.crm.dto.ProductResponse;
import com.salon.crm.entity.Product;
import com.salon.crm.exception.BadRequestException;
import com.salon.crm.exception.NotFoundException;
import com.salon.crm.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(Boolean activeOnly, Boolean lowStockOnly) {
        List<Product> products = Boolean.TRUE.equals(activeOnly)
                ? productRepository.findByActiveTrue()
                : productRepository.findAll();
        return products.stream()
                .filter(p -> !Boolean.TRUE.equals(lowStockOnly) || isLow(p))
                .map(this::toResponse).toList();
    }

    public ProductResponse create(ProductRequest req) {
        Product p = new Product();
        apply(p, req);
        return toResponse(save(p));
    }

    public ProductResponse update(UUID id, ProductRequest req) {
        Product p = find(id);
        apply(p, req);
        return toResponse(save(p));
    }

    public void delete(UUID id) {
        Product p = find(id);
        p.setActive(false);
        productRepository.save(p);
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name());
        p.setSku(req.sku() != null && !req.sku().isBlank() ? req.sku() : null);
        p.setCategory(req.category());
        p.setPrice(req.price());
        if (req.stockQty() != null) p.setStockQty(req.stockQty());
        if (req.lowStockThreshold() != null) p.setLowStockThreshold(req.lowStockThreshold());
        if (req.active() != null) p.setActive(req.active());
    }

    private Product save(Product p) {
        try {
            return productRepository.saveAndFlush(p);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BadRequestException("SKU already exists: " + p.getSku());
        }
    }

    private boolean isLow(Product p) {
        return p.getStockQty() <= p.getLowStockThreshold();
    }

    private Product find(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getSku(), p.getCategory(),
                p.getPrice(), p.getStockQty(), p.getLowStockThreshold(), p.isActive(), isLow(p));
    }
}
