package com.mysite.restaurant.hj.domain.entity;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    private long userId;
    private String userName;
    private String password;
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private boolean notificationAgreed;
    private UserType userType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isDeleted;
    private UserStatus status;
    private LocalDateTime lastLoginAt;
}