package com.daon.flight_booking.user.service;

import com.daon.flight_booking.user.domain.User;
import com.daon.flight_booking.user.dto.CreateUserRequest;
import com.daon.flight_booking.user.dto.UserResponse;
import com.daon.flight_booking.user.exception.DuplicateUserException;
import com.daon.flight_booking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    void createUser_validRequest_returnsMappedResponse() {
        User saved = User.builder().id(1L).email("alice@example.com").name("Alice Murphy").build();
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(saved);

        UserResponse response = userService.createUser(buildRequest("alice@example.com", "Alice Murphy"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getName()).isEqualTo("Alice Murphy");
    }

    @Test
    void createUser_duplicateEmail_throws409() {
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> userService.createUser(buildRequest("alice@example.com", "Alice Murphy")))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("alice@example.com");
    }

    private CreateUserRequest buildRequest(String email, String name) {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(email);
        request.setName(name);
        request.setUsername("alice");
        request.setPassword("password1");
        return request;
    }
}
