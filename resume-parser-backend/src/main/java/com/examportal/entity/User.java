package com.examportal.entity;

import com.examportal.enums.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String password;
    private Role role;
    @Builder.Default
    private String createdAt = java.time.LocalDateTime.now().toString();
}
