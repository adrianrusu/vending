package com.adrianr.vending.service;

import com.adrianr.vending.domain.Product;
import com.adrianr.vending.domain.User;
import com.adrianr.vending.repository.ProductRepository;
import com.adrianr.vending.repository.UserRepository;
import com.adrianr.vending.rest.dto.BuyResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class VendingService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SecurityService securityService;

    public VendingService(UserRepository userRepository,
                          ProductRepository productRepository,
                          SecurityService securityService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.securityService = securityService;
    }

    public User deposit(Integer amount) {
        User user = userRepository.getById(securityService.getLoggedUserId());

        user.setDeposit(user.getDeposit().add(BigDecimal.valueOf(amount)));

        return userRepository.save(user);
    }

    public BuyResponseDto buy(Integer productId, Integer amount) {
        User user = userRepository.getById(securityService.getLoggedUserId());
        var productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product id does not exist");
        }
        Product product = productOptional.get();

        if (product.getAmountAvailable() < amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested amount is greater than stock");
        }

        BigDecimal totalAmount = product.getCost().multiply(BigDecimal.valueOf(amount));
        if (user.getDeposit().compareTo(totalAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough funds to complete the order");
        }

        product.setAmountAvailable(product.getAmountAvailable() - amount);
        productRepository.save(product);

        user.setDeposit(user.getDeposit().subtract(totalAmount));
        userRepository.save(user);

        return BuyResponseDto.builder()
                .productName(product.getProductName())
                .amount(amount)
                .totalPrice(totalAmount)
                .change(user.getDeposit()
                        .divide(BigDecimal.valueOf(5), RoundingMode.DOWN)
                        .setScale(0, RoundingMode.DOWN)
                        .multiply(BigDecimal.valueOf(5)).intValue())
                .build();
    }

    public User reset() {
        User user = userRepository.getById(securityService.getLoggedUserId());

        user.setDeposit(BigDecimal.ZERO);
        return userRepository.save(user);
    }

}
