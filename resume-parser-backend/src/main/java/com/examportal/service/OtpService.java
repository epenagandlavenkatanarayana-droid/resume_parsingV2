package com.examportal.service;

public interface OtpService {
    void generateAndSendOtp(String email);
    boolean verifyOtp(String email, String otp);
}
