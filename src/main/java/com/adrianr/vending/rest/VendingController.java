package com.adrianr.vending.rest;

import com.adrianr.vending.domain.User;
import com.adrianr.vending.rest.dto.BuyResponseDto;
import com.adrianr.vending.rest.dto.UserDto;
import com.adrianr.vending.service.VendingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ROLE_BUYER')")
public class VendingController {

    private final VendingService vendingService;

    public VendingController(VendingService vendingService) {
        this.vendingService = vendingService;
    }

    @PutMapping("deposit/{amount:5|10|20|50|100}")
    public ResponseEntity<UserDto> deposit(@PathVariable Integer amount) {
        User user = vendingService.deposit(amount);

        return ResponseEntity.ok(UserDto.fromUser(user));
    }

    @PutMapping("buy")
    public ResponseEntity<BuyResponseDto> buy(@RequestParam Integer productId,
                                              @RequestParam Integer amount) {
        return ResponseEntity.ok(vendingService.buy(productId, amount));
    }

    @PutMapping("reset")
    public ResponseEntity<UserDto> reset() {
        return ResponseEntity.ok(UserDto.fromUser(vendingService.reset()));
    }

}
