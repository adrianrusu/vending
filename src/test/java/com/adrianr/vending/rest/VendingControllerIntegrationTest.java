package com.adrianr.vending.rest;

import com.adrianr.vending.domain.UserRole;
import com.adrianr.vending.repository.ProductRepository;
import com.adrianr.vending.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class VendingControllerIntegrationTest {

    private static final String BUYER_USERNAME = "buyer";
    private static final String BUYER_ROLE = "BUYER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void givenNoAuth_whenCallingDeposit_ThenExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(post("/deposit/10"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthAndNoRole_whenCallingDeposit_thenExpectForbiddenResponseStatus() throws Exception {
        mockMvc.perform(post("/deposit/10"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = BUYER_ROLE)
    void givenAuthAndBuyerRoleAndInvalidAmount_whenCallingDeposit_thenNotFoundResponseStatus() throws Exception {
        mockMvc.perform(post("/deposit/3"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext
    @Transactional
    @WithMockUser(roles = BUYER_ROLE, username = BUYER_USERNAME)
    void givenAuthAndBuyerRoleWithValidAmount_whenCallingDeposit_thenOkResponseStatusAndUpdatedBalance() throws Exception {
        mockMvc.perform(post("/deposit/10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.username").value(BUYER_USERNAME))
                .andExpect(jsonPath("$.role").value(UserRole.BUYER.getName()))
                .andExpect(jsonPath("$.deposit").value(25));

        assertEquals(BigDecimal.valueOf(25), userRepository.getById(8).getDeposit());
    }

    @Test
    void givenNoAuth_whenCallingBuy_thenExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(post("/buy"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthAndNoParameters_whenCallingBuy_thenExpectBadRequestResponseStatus() throws Exception {
        mockMvc.perform(post("/buy"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = BUYER_ROLE, username = BUYER_USERNAME)
    void givenAuthAndInvalidParameters_whenCallingBuy_thenExpectBadRequestResponseStatus() {
        Exception e = assertThrows(NestedServletException.class, () -> mockMvc.perform(post("/buy")
                .queryParam("productId", "1")
                .queryParam("amount", "0")));

        assertTrue(e.getCause() instanceof ConstraintViolationException);
    }

    @Test
    @WithMockUser(roles = BUYER_ROLE, username = BUYER_USERNAME)
    void givenAuthAndNonExistingProductId_whenCallingBuy_thenExpectNotFoundResponseStatus() throws Exception {
        mockMvc.perform(post("/buy")
                        .queryParam("productId", "-1")
                        .queryParam("amount", "1"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = BUYER_ROLE, username = BUYER_USERNAME)
    void givenAuthAndGreaterAmountThanAvailable_whenCallingBuy_thenExpectBadRequestResponseStatus() throws Exception {
        mockMvc.perform(post("/buy")
                        .queryParam("productId", "1")
                        .queryParam("amount", "20"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = BUYER_ROLE, username = BUYER_USERNAME)
    void givenAuthAndInsufficientBalance_whenCallingBuy_thenExpectBadRequestResponseStatus() throws Exception {
        mockMvc.perform(post("/buy")
                        .queryParam("productId", "1")
                        .queryParam("amount", "3"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DirtiesContext
    @Transactional
    @WithMockUser(roles = BUYER_ROLE, username = BUYER_USERNAME)
    void givenAuthAndBalance_whenCallingBuy_thenExpectOkResponseStatusAndChangeAmountInResponseBody() throws Exception {
        Integer initialAmount = productRepository.getById(1).getAmountAvailable();

        mockMvc.perform(post("/buy")
                        .queryParam("productId", "1")
                        .queryParam("amount", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(12))
                .andExpect(jsonPath("$.productName").value("Coca Cola"))
                .andExpect(jsonPath("$.amount").value(1))
                .andExpect(jsonPath("$.change").value(0));

        assertEquals(BigDecimal.valueOf(3), userRepository.getById(8).getDeposit());
        assertEquals(initialAmount - 1, productRepository.getById(1).getAmountAvailable());
    }

    @Test
    void givenNoAuth_whenCallingReset_thenExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(post("/reset"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthAndNoRole_whenCallingReset_thenExpectForbiddenResponseStatus() throws Exception {
        mockMvc.perform(post("/reset"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DirtiesContext
    @Transactional
    @WithMockUser(roles = BUYER_ROLE, username = "test-buyer")
    void givenAuthAndRole_whenCallingReset_thenExpectOkResponseStatusAndChangeBody() throws Exception {
        mockMvc.perform(post("/reset"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newDeposit").value(0))
                .andExpect(jsonPath("$.change.['100']").value(1))
                .andExpect(jsonPath("$.change.['50']").value(0))
                .andExpect(jsonPath("$.change.['20']").value(1))
                .andExpect(jsonPath("$.change.['10']").value(0))
                .andExpect(jsonPath("$.change.['5']").value(1));

        assertEquals(BigDecimal.ZERO, userRepository.getById(11).getDeposit());
    }

}
