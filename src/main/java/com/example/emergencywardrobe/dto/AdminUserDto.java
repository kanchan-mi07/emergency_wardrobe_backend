package com.example.emergencywardrobe.dto;

import com.example.emergencywardrobe.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String name;
    private String email;
    private String role;
    private LocalDateTime createdAt;

    public static AdminUserDto fromEntity(User user) {
        return new AdminUserDto(user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), user.getCreatedAt());
    }
}