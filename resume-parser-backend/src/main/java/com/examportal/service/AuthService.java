package com.examportal.service;

import com.examportal.payload.AuthResponse;
import com.examportal.payload.OtpLoginRequest;
import com.examportal.payload.OtpRequest;

public interface AuthService {
    void sendOtp(OtpRequest otpRequest);
    AuthResponse login(OtpLoginRequest loginRequest);
}
