package com.adrianr.vending.rest;


import com.adrianr.vending.domain.User;
import com.adrianr.vending.rest.dto.CreateUserDto;
import com.adrianr.vending.rest.dto.UpdateUserDto;
import com.adrianr.vending.rest.dto.UserDto;
import com.adrianr.vending.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers()
                .stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList()));
    }

    @GetMapping("{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(UserDto.fromUser(userService.getUserById(id)));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserDto userDto) {
        User user = userService.createUser(userDto.toUser());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(user.getId()).toUri();

        return ResponseEntity.created(location).body(UserDto.fromUser(user));
    }

    @PutMapping
    public ResponseEntity<UserDto> updateUser(@Valid @RequestBody UpdateUserDto userDto) {
        return ResponseEntity.ok(UserDto.fromUser(userService.updateUser(userDto.getUsername(), userDto.getPassword())));
    }

    @DeleteMapping
    public ResponseEntity<HttpStatus> deleteUser() {
        userService.deleteUser();
        return ResponseEntity.ok().build();
    }

}
