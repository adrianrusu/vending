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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
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
class UserControllerIntegrationTest {

    private static final String BUYER_USERNAME = "buyer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void givenNoAuthentication_WhenCallingGetAllUsers_ThenExpectForbiddenResponseStatus() throws Exception {
        mockMvc.perform(get("/users"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthentication_WhenCallingGetAllUsers_ThenExpectOKResponseStatusAndResultList() throws Exception {
        List<User> result = userRepository.findAll();

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(result.size()))
                .andExpect(jsonPath("$.[0].id").value(result.get(0).getId()))
                .andExpect(jsonPath("$.[0].username").value(result.get(0).getUsername()))
                .andExpect(jsonPath("$.[0].role").value(result.get(0).getRole().getName()))
                .andExpect(jsonPath("$.[0].deposit").value(result.get(0).getDeposit()))
                .andExpect(jsonPath("$.[0].password").doesNotExist());
    }

    @Test
    void givenNotAuthenticated_WhenCallingGetUserById_ThenExpectForbiddenResponseStatus() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @Transactional
    void givenAuthentication_WhenCallingGetUserById_ThenExpectOKResponseStatusAndResult() throws Exception {
        User result = userRepository.getById(8);

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
    void givenAuthentication_WhenCallingGetUserByNonExistingId_ThenExpectNotFoundResponseStatus() throws Exception {
        mockMvc.perform(get("/users/-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNoPayload_WhenCallingCreateUser_ThenExpectBadResponseStatus() throws Exception {
        mockMvc.perform(post("/users"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenInvalidPayload_WhenCallingCreateUser_ThenExpectBadResponseStatus() throws Exception {
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
    void givenInvalidRoleInPayload_WhenCallingCreateUser_ThenExpectBadResponseStatus() throws Exception {
        HashMap<String, String> payload = new HashMap<>();
        payload.put("username", "abc");
        payload.put("password", "abc");
        payload.put("role", "abc");

        mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenUsernameThatAlreadyExists_WhenCallingCreateUser_ThenExpectConflictResponseStatus() throws Exception {
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
    void givenValidPayload_WhenCallingCreateUser_ThenExpectOkResponseStatusAndPayloadWithIdAndNoPassword() throws Exception {
        CreateUserDto payload = CreateUserDto.builder()
                .username("test-user")
                .password("test-password")
                .role(UserRole.BUYER)
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(payload.getUsername()))
                .andExpect(jsonPath("$.role").value(UserRole.BUYER.getName()))
                .andExpect(jsonPath("$.deposit").value(0))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void givenNoAuth_WhenCallingUpdateUser_ThenExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .contentType(objectMapper.writeValueAsString(emptyMap())))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void givenAuthAndInvalidPayload_WhenCallingUpdateUser_ThenExpectBadResponseStatus() throws Exception {
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyMap())))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errors.username").value("must not be null"));
    }

    @Test
    @DirtiesContext
    @Transactional
    @WithMockUser(username = BUYER_USERNAME)
    void givenAuthAndNoPasswordInPayload_WhenCallingUpdateUser_ThenExpectOkResponseStatusAndSamePassword() throws Exception {
        String initialPassword = userRepository.getById(8).getPassword();
        String expectedUsername = "bob";

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateUserDto.builder()
                                .username(expectedUsername)
                                .build())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(expectedUsername))
                .andExpect(jsonPath("$.deposit").value(15))
                .andExpect(jsonPath("$.role").value(UserRole.BUYER.getName()));

        User updatedUser = userRepository.getById(8);
        assertEquals(initialPassword, updatedUser.getPassword());
        assertEquals(expectedUsername, updatedUser.getUsername());
    }

    @Test
    @WithMockUser(username = BUYER_USERNAME)
    void givenAuthAndSameUsernameAsExistingUser_WhenCallingUpdateUser_ThenExpectConflictingResponseStatus() throws Exception {
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
    @Transactional
    @WithMockUser(username = BUYER_USERNAME)
    void givenAuthAndDifferentPassword_WhenCallingUpdateUser_ThenExpectOkResponseStatusAndDifferentPassword() throws Exception {
        User initialUser = userRepository.getById(8);
        String initialPassword = initialUser.getPassword();
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
                .andExpect(jsonPath("$.deposit").value(15))
                .andExpect(jsonPath("$.role").value(UserRole.BUYER.getName()));

        User updatedUser = userRepository.getById(8);
        assertEquals(initialUser.getUsername(), updatedUser.getUsername());
        assertNotEquals(initialPassword, updatedUser.getPassword());
    }

    @Test
    void givenNoAuth_WhenCallingDeleteUser_ThenExpectUnauthorizedResponseStatus() throws Exception {
        mockMvc.perform(delete("/users"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = BUYER_USERNAME)
    @DirtiesContext
    void givenAuth_WhenCallingDeleteUser_ThenExpectOkStatusAndUserNotPresentInDatabase() throws Exception {
        mockMvc.perform(delete("/users"))
                .andDo(print())
                .andExpect(status().isOk());

        Optional<User> buyer = userRepository.findByUsername("buyer");
        assertTrue(buyer.isEmpty());
    }

}
