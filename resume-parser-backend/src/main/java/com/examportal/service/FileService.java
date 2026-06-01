package com.examportal.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String uploadResume(MultipartFile file);
}
