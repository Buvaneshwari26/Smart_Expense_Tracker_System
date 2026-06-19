package com.tracker.service;

import com.tracker.dto.UserProfileDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.User;
import com.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(Long userId) {
        User user = getUserEntity(userId);
        return mapToProfile(user);
    }

    @Transactional
    public UserProfileDTO updateProfile(Long userId, UserProfileDTO dto) {
        User user = getUserEntity(userId);
        user.setFullName(dto.getFullName());
        user.setUsername(dto.getUsername());
        user.setPhoneNumber(dto.getPhoneNumber());
        return mapToProfile(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserEntity(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user {}", userId);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToProfile).collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserEntity(userId);
        userRepository.delete(user); // Soft delete
        log.info("User soft-deleted: {}", userId);
    }

    private UserProfileDTO mapToProfile(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
