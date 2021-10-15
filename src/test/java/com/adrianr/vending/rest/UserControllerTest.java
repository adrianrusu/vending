package com.adrianr.vending.rest;

import com.adrianr.vending.domain.User;
import com.adrianr.vending.domain.UserRole;
import com.adrianr.vending.repository.UserRepository;
import com.adrianr.vending.rest.dto.CreateUserDto;
import com.adrianr.vending.rest.dto.UpdateUserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void givenNoAuthentication_WhenCallingGetAllUsers_ExpectForbiddenResponseStatus() throws Exception {
        mockMvc.perform(get("/users"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthentication_WhenCallingGetAllUsers_ExpectOKResponseStatusAndResultList() throws Exception {
        List<User> result = userRepository.findAll();

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].id").value(result.get(0).getId()))
                .andExpect(jsonPath("$.[0].username").value(result.get(0).getUsername()))
                .andExpect(jsonPath("$.[0].role").value(result.get(0).getRole().getName()))
                .andExpect(jsonPath("$.[0].deposit").value(result.get(0).getDeposit()))
                .andExpect(jsonPath("$.[0].password").doesNotExist());
    }

    @Test
    void givenNotAuthenticated_WhenCallingGetUserById_ExpectForbiddenResponseStatus() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthentication_WhenCallingGetUserById_ExpectOKResponseStatusAndResult() throws Exception {
        User result = userRepository.findById(8).get();

        mockMvc.perform(get("/users/8"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(result.getId()))
                .andExpect(jsonPath("$.username").value(result.getUsername()))
                .andExpect(jsonPath("$.role").value(result.getRole().getName()))
                .andExpect(jsonPath("$.deposit").value(result.getDeposit()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser
    void givenAuthentication_WhenCallingGetUserByNonExistingId_ExpectNotFoundResponseStatus() throws Exception {
        mockMvc.perform(get("/users/-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNoPayload_WhenCallingCreateUser_ExpectBadResponseStatus() throws Exception {
        mockMvc.perform(post("/users"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenInvalidPayload_WhenCallingCreateUser_ExpectBadResponseStatus() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyMap()))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errors.username").value("must not be null"))
                .andExpect(jsonPath("$.errors.password").value("must not be null"))
                .andExpect(jsonPath("$.errors.role").value("must not be null"));
    }

    @Test
    void givenInvalidRoleInPayload_WhenCallingCreateUser_ExpectBadResponseStatus() throws Exception {
        HashMap<String, String> userWithInvalidRole = new HashMap<>();
        userWithInvalidRole.put("username", "abc");
        userWithInvalidRole.put("password", "abc");
        userWithInvalidRole.put("role", "abc");

        mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userWithInvalidRole)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenUsernameThatAlreadyExists_WhenCallingCreateUser_ExpectConflictResponseStatus() throws Exception {
        CreateUserDto payload = CreateUserDto.builder()
                .username("buyer")
                .password("test-password")
                .role(UserRole.BUYER)
                .build();

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DirtiesContext
    void givenValidPayload_WhenCallingCreateUser_ExpectOkResponseStatusAndPayloadWithIdAndNoPassword() throws Exception {
        CreateUserDto payload = CreateUserDto.builder()
                .username("test-buyer")
                .password("test-password")
                .role(UserRole.BUYER)
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(payload.getUsername()))
                .andExpect(jsonPath("$.role").value(UserRole.BUYER.getName()))
                .andExpect(jsonPath("$.deposit").value(0))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void givenNoAuth_WhenCallingUpdateUser_ExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .contentType(objectMapper.writeValueAsString(emptyMap())))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthAndInvalidPayload_WhenCallingUpdateUser_ExpectBadResponseStatus() throws Exception {
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyMap())))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errors.username").value("must not be null"));
    }

    @Test
    @WithMockUser(username = "buyer")
    @DirtiesContext
    void givenAuthAndNoPasswordInPayload_WhenCallingUpdateUser_ExpectOkResponseStatusAndSamePassword() throws Exception {
        String initialPassword = userRepository.findById(8).get().getPassword();
        String expectedUsername = "bob";

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateUserDto.builder()
                                .username(expectedUsername)
                                .build())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(expectedUsername))
                .andExpect(jsonPath("$.deposit").value(0))
                .andExpect(jsonPath("$.role").value(UserRole.BUYER.getName()));

        User updatedUser = userRepository.findById(8).get();
        assertEquals(initialPassword, updatedUser.getPassword());
        assertEquals(expectedUsername, updatedUser.getUsername());
    }

    @Test
    @WithMockUser(username = "buyer")
    void givenAuthAndSameUsernameAsExistingUser_WhenCallingUpdateUser_ExpectConflictingResponseStatus() throws Exception {
        String expectedUsername = "seller";

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateUserDto.builder()
                                .username(expectedUsername)
                                .build())))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "buyer")
    void givenAuthAndDifferentPassword_WhenCallingUpdateUser_ExpectOkResponseStatusAndDifferentPassword() throws Exception {
        User initialUser = userRepository.findById(8).get();
        String newPassword = "bob";

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateUserDto.builder()
                                .username(initialUser.getUsername())
                                .password(newPassword)
                                .build())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(initialUser.getUsername()))
                .andExpect(jsonPath("$.deposit").value(0))
                .andExpect(jsonPath("$.role").value(UserRole.BUYER.getName()));

        User updatedUser = userRepository.findById(8).get();
        assertEquals(initialUser.getUsername(), updatedUser.getUsername());
        assertNotEquals(initialUser.getPassword(), updatedUser.getPassword());
    }

    @Test
    void givenNoAuth_WhenCallingDeleteUser_ExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(delete("/users"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "buyer")
    @DirtiesContext
    void givenAuth_WhenCallingDeleteUser_ExpectOkStatusAndUserNotPresentInDatabase() throws Exception {
        mockMvc.perform(delete("/users"))
                .andDo(print())
                .andExpect(status().isOk());

        Optional<User> buyer = userRepository.findByUsername("buyer");
        assertTrue(buyer.isEmpty());
    }

}
