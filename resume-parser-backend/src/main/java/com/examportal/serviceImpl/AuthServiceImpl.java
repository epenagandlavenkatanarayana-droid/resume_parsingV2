package com.examportal.serviceImpl;

import com.examportal.entity.User;
import com.examportal.enums.Role;
import com.examportal.payload.AuthResponse;
import com.examportal.payload.OtpLoginRequest;
import com.examportal.payload.OtpRequest;
import com.examportal.repository.UserRepository;
import com.examportal.security.CustomUserDetails;
import com.examportal.security.JwtUtil;
import com.examportal.service.AuthService;
import com.examportal.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void sendOtp(OtpRequest otpRequest) {
        String email = otpRequest.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Access denied. Email is not registered."));

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.HR) {
            throw new IllegalArgumentException("Access denied. Only Admin or HR users can log in.");
        }

        otpService.generateAndSendOtp(email);
    }

    @Override
    public AuthResponse login(OtpLoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String otp = loginRequest.getOtp();

        // 1. Verify OTP
        boolean isValid = otpService.verifyOtp(email, otp);
        if (!isValid) {
            throw new IllegalArgumentException("Invalid or expired OTP. Please try again.");
        }

        // 2. Fetch User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // 3. Double check role (though checked at sendOtp)
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.HR) {
            throw new IllegalArgumentException("Access denied. Only Admin or HR users can log in.");
        }

        // 4. Generate Token
        String token = jwtUtil.generateToken(new CustomUserDetails(user));
        return new AuthResponse(token, user.getRole(), user.getFullName());
    }
}
