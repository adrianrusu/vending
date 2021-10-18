package com.adrianr.vending.rest;

import com.adrianr.vending.domain.Product;
import com.adrianr.vending.repository.ProductRepository;
import com.adrianr.vending.rest.dto.ProductDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class ProductControllerIntegrationTest {

    private static final String SELLER_ROLE = "SELLER";
    private static final String SELLER_USERNAME = "seller";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void givenNoAuth_whenCallingGetAllProducts_ThenExpectOkResponseStatusAndResultList() throws Exception {
        List<Product> result = productRepository.findAll();

        mockMvc.perform(get("/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(result.size()))
                .andExpect(jsonPath("$.[0].id").value(result.get(0).getId()))
                .andExpect(jsonPath("$.[0].productName").value(result.get(0).getProductName()))
                .andExpect(jsonPath("$.[0].sellerId").value(result.get(0).getSellerId()))
                .andExpect(jsonPath("$.[0].cost").value(result.get(0).getCost()))
                .andExpect(jsonPath("$.[0].amountAvailable").value(result.get(0).getAmountAvailable()));
    }

    @Test
    void givenNoAuthAndNonExistingProductId_whenCallingGetProductById_ThenExpectNotFoundResponseStatus() throws Exception {
        mockMvc.perform(get("/products/-1"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNoAuthAndExistingProductId_whenCallingGetProductById_ThenExpectOkResponseStatusAndProductInResponseBody() throws Exception {
        Product product = productRepository.findAll().get(0);

        mockMvc.perform(get("/products/" + product.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.productName").value(product.getProductName()))
                .andExpect(jsonPath("$.sellerId").value(product.getSellerId()))
                .andExpect(jsonPath("$.cost").value(product.getCost()))
                .andExpect(jsonPath("$.amountAvailable").value(product.getAmountAvailable()));
    }

    @Test
    void givenNoAuth_whenCallingCreateProduct_ThenExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyMap())))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthAndInvalidProductAndNullAmounts_whenCallingCreateProduct_ThenExpectBadRequestResponseStatus() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyMap())))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errors.productName").value("must not be null"))
                .andExpect(jsonPath("$.errors.cost").value("must not be null"))
                .andExpect(jsonPath("$.errors.amountAvailable").value("must not be null"));
    }

    @Test
    @WithMockUser
    void givenAuthAndInvalidProductWithNegativeAmounts_whenCallingCreateProduct_ThenExpectBadRequestResponseStatus() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productName("Test Product")
                .cost(BigDecimal.TEN.negate())
                .amountAvailable(-10)
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errors.cost").value("must be greater than or equal to 1"))
                .andExpect(jsonPath("$.errors.amountAvailable").value("must be greater than or equal to 1"));
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void givenAuthWithBuyerRole_whenCallingCreateProduct_ThenExpectForbiddenResponseStatus() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productName("Test Product")
                .cost(BigDecimal.TEN)
                .amountAvailable(1)
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DirtiesContext
    @WithMockUser(roles = SELLER_ROLE, username = SELLER_USERNAME)
    void givenAuthWithSellerRole_whenCallingCreateProduct_ThenExpectCreatedResponseStatusAndCreatedProductAsBody() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productName("Test Product")
                .cost(BigDecimal.TEN)
                .amountAvailable(1)
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.productName").value(payload.getProductName()))
                .andExpect(jsonPath("$.cost").value(payload.getCost()))
                .andExpect(jsonPath("$.amountAvailable").value(payload.getAmountAvailable()))
                .andExpect(jsonPath("$.sellerId").exists());
    }

    @Test
    void givenNoAuth_whenCallingUpdateProduct_ThenExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(put("/products/1"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthAndInvalidProductWithEmpty_whenCallingUpdateProduct_ThenExpectBadRequestResponseStatus() throws Exception {
        mockMvc.perform(put("/products/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyMap())))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errors.productName").value("must not be null"))
                .andExpect(jsonPath("$.errors.cost").value("must not be null"))
                .andExpect(jsonPath("$.errors.amountAvailable").value("must not be null"));
    }

    @Test
    @WithMockUser
    void givenAuthAndInvalidProductWithNegativeAmounts_whenCallingUpdateProduct_ThenExpectBadRequestResponseStatus() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productName("Test Product")
                .cost(BigDecimal.TEN.negate())
                .amountAvailable(-10)
                .build();

        mockMvc.perform(put("/products/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errors.cost").value("must be greater than or equal to 1"))
                .andExpect(jsonPath("$.errors.amountAvailable").value("must be greater than or equal to 1"));
    }

    @Test
    @WithMockUser
    void givenAuthWithNoRoleAndValidProduct_whenCallingUpdateProduct_ThenExpectForbiddenResponseStatus() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productName("Test Product")
                .cost(BigDecimal.TEN)
                .amountAvailable(10)
                .build();

        mockMvc.perform(put("/products/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = SELLER_ROLE)
    void givenAuthWithSellerRoleAndInvalidProductId_whenCallingUpdateProduct_ThenExpectNotFoundResponseStatus() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productName("Test Product")
                .cost(BigDecimal.TEN)
                .amountAvailable(10)
                .build();

        mockMvc.perform(put("/products/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(roles = SELLER_ROLE, username = SELLER_USERNAME)
    void givenAuthWithSellerRoleAndValidProductAndDifferentSellerId_whenCallingUpdateProduct_ThenExpectForbiddenResponseStatus() throws Exception {
        Product initialProduct = productRepository.getById(4);
        ProductDto payload = ProductDto.builder()
                .productName("Test Product")
                .cost(BigDecimal.TEN)
                .amountAvailable(10)
                .build();

        mockMvc.perform(put("/products/" + initialProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DirtiesContext
    @WithMockUser(roles = SELLER_ROLE, username = SELLER_USERNAME)
    void givenAuthWithSellerRoleAndValidProduct_whenCallingUpdateProduct_ThenExpectOkResponseStatusAndUpdatedProductInBody() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productName("Test Product")
                .cost(BigDecimal.TEN)
                .amountAvailable(10)
                .build();

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value(payload.getProductName()))
                .andExpect(jsonPath("$.cost").value(payload.getCost()))
                .andExpect(jsonPath("$.amountAvailable").value(payload.getAmountAvailable()))
                .andExpect(jsonPath("$.sellerId").value(9));

        Product expectedProduct = payload.toProduct(1);
        expectedProduct.setSellerId(9);

        Product finalProduct = productRepository.getById(1);
        assertEquals(expectedProduct, finalProduct);
    }

    @Test
    void givenNoAuth_whenCallingDeleteProduct_ThenExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthWithNoRole_whenCallingDeleteProduct_ThenExpectForbiddenResponseStatus() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = SELLER_ROLE)
    void givenAuthAndNonExistingProduct_whenCallingDeleteProduct_ThenExpectNotFoundResponseStatus() throws Exception {
        mockMvc.perform(delete("/products/-1"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = SELLER_ROLE, username = SELLER_USERNAME)
    void givenAuthAndDifferentSeller_whenCallingDeleteProduct_ThenExpectForbiddenResponseStatus() throws Exception {
        mockMvc.perform(delete("/products/4"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DirtiesContext
    @WithMockUser(roles = SELLER_ROLE, username = SELLER_USERNAME)
    void givenAuthAndCorrectSellerAndProductId_whenCallingDeleteProduct_ThenExpectOkResponseStatusAndProductRemoved() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andDo(print())
                .andExpect(status().isOk());

        assertTrue(productRepository.findById(1).isEmpty());
    }

}
