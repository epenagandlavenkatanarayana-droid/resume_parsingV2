package com.examportal.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {
    private String id;
    private String fileName;
    private String filePath;
    private String userId;
    @Builder.Default
    private String uploadedAt = java.time.LocalDateTime.now().toString();
}
