package com.adrianr.vending.service;

import com.adrianr.vending.domain.User;
import com.adrianr.vending.repository.ProductRepository;
import com.adrianr.vending.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;

    public UserService(UserRepository userRepository,
                       ProductRepository productRepository,
                       PasswordEncoder passwordEncoder,
                       SecurityService securityService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityService = securityService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User createUser(User user) {
        if (userRepository.findByUsername(user.getUsername().toLowerCase()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        user.setDeposit(BigDecimal.ZERO);
        user.setUsername(user.getUsername().toLowerCase());
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public User updateUser(String username, String password) {
        Optional<User> userByUsername = userRepository.findByUsername(username);
        if (userByUsername.isPresent() && !userByUsername.get().getId().equals(securityService.getLoggedUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User user = userRepository.getById(securityService.getLoggedUserId());
        user.setUsername(username.toLowerCase());

        if (nonNull(password)) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(user);
    }

    public void deleteUser() {
        Integer loggedUserId = securityService.getLoggedUserId();

        productRepository.deleteAllBySellerId(loggedUserId);
        userRepository.deleteById(loggedUserId);
    }
}
