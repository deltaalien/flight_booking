package com.daon.flight_booking.user.service;

import com.daon.flight_booking.user.domain.Role;
import com.daon.flight_booking.user.domain.User;
import com.daon.flight_booking.user.dto.CreateUserRequest;
import com.daon.flight_booking.user.dto.UserResponse;
import com.daon.flight_booking.user.exception.DuplicateUserException;
import com.daon.flight_booking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        try {
            return toResponse(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUserException(request.getEmail());
        }
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
