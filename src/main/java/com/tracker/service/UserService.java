package com.tracker.service;

import com.tracker.dto.AuthResponse;
import com.tracker.dto.UserLoginRequest;
import com.tracker.dto.UserRegisterRequest;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.User;
import com.tracker.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse registerUser(UserRegisterRequest request) {
        // 1. Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        // 2. Hash the password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Save User
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(hashedPassword)
                .build();

        User savedUser = userRepository.save(user);

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .token("MOCK_JWT_TOKEN_" + savedUser.getId()) // Optional mock token representation
                .message("User registered successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse loginUser(UserLoginRequest request) {
        // 1. Fetch user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // 2. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token("MOCK_JWT_TOKEN_" + user.getId())
                .message("Login successful")
                .build();
    }

    @Transactional(readOnly = true)
    public User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
