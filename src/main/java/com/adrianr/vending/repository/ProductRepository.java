package com.adrianr.vending.repository;

import com.adrianr.vending.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    void deleteAllBySellerId(Integer sellerId);
}
