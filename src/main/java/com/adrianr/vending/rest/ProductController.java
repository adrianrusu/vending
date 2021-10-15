package com.adrianr.vending.rest;


import com.adrianr.vending.domain.Product;
import com.adrianr.vending.rest.dto.ProductDto;
import com.adrianr.vending.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_SELLER')")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductDto productDto) {
        Product product = productService.createProduct(productDto.toProduct());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(product.getId()).toUri();

        return ResponseEntity.created(location).body(product);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasRole('ROLE_SELLER')")
    public ResponseEntity<Product> updateProduct(@PathVariable Integer id, @Valid @RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.updateProduct(productDto.toProduct(id)));
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ROLE_SELLER')")
    public ResponseEntity<HttpStatus> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }
}
