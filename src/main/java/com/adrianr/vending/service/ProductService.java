package com.adrianr.vending.service;

import com.adrianr.vending.domain.Product;
import com.adrianr.vending.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SecurityService securityService;

    public ProductService(ProductRepository productRepository, SecurityService securityService) {
        this.productRepository = productRepository;
        this.securityService = securityService;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProduct(Integer id) {
        return productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Product createProduct(Product product) {
        product.setSellerId(securityService.getLoggedUserId());

        return productRepository.save(product);
    }

    public Product updateProduct(Product product) {
        Optional<Product> dbProduct = productRepository.findById(product.getId());
        if (dbProduct.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product id does not exist");
        }
        if (!securityService.getLoggedUserId().equals(dbProduct.get().getSellerId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        product.setSellerId(securityService.getLoggedUserId());

        return productRepository.save(product);
    }

    public void deleteProduct(Integer productId) {
        Optional<Product> dbProduct = productRepository.findById(productId);
        if (dbProduct.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product id does not exist");
        }
        if (!securityService.getLoggedUserId().equals(dbProduct.get().getSellerId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        productRepository.deleteById(productId);
    }

}
