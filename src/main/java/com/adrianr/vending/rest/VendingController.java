package com.adrianr.vending.rest;

import com.adrianr.vending.domain.User;
import com.adrianr.vending.rest.dto.BuyResponseDto;
import com.adrianr.vending.rest.dto.ChangeDto;
import com.adrianr.vending.rest.dto.UserDto;
import com.adrianr.vending.service.VendingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;


@Validated
@RestController
@PreAuthorize("hasRole('ROLE_BUYER')")
public class VendingController {

    private final VendingService vendingService;

    public VendingController(VendingService vendingService) {
        this.vendingService = vendingService;
    }

    @PostMapping("deposit/{amount:5|10|20|50|100}")
    public ResponseEntity<UserDto> deposit(@PathVariable Integer amount) {
        User user = vendingService.deposit(amount);

        return ResponseEntity.ok(UserDto.fromUser(user));
    }

    @PostMapping("buy")
    public ResponseEntity<BuyResponseDto> buy(@RequestParam Integer productId,
                                              @RequestParam @Min(1) Integer amount) {
        return ResponseEntity.ok(vendingService.buy(productId, amount));
    }

    @PostMapping("reset")
    public ResponseEntity<ChangeDto> reset() {
        return ResponseEntity.ok(ChangeDto.builder().change(vendingService.reset()).build());
    }

}
