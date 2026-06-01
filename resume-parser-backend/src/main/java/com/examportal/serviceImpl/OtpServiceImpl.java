package com.examportal.serviceImpl;

import com.examportal.service.OtpService;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpServiceImpl implements OtpService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    // In-memory cache for OTPs: email -> OtpData
    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Getter
    @AllArgsConstructor
    private static class OtpData {
        private final String otpCode;
        private final LocalDateTime expiryTime;
    }

    @Override
    public void generateAndSendOtp(String email) {
        // Generate a 6-digit OTP
        int otpNum = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(otpNum);
        
        // Cache OTP with 5 minutes expiration
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);
        otpCache.put(email, new OtpData(otp, expiry));

        // Always print OTP in console for easy debugging and development bypass
        log.info("\n==================================================" +
                 "\n=== [OTP SERVICE] OTP for: {}" +
                 "\n=== Code: {}" +
                 "\n==================================================", email, otp);

        // Try to send email
        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(email);
                helper.setSubject("ResumeParser - Login OTP Verification");
                
                String htmlContent = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 500px; margin: 0 auto; padding: 30px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);\">"
                        + "  <div style=\"text-align: center; margin-bottom: 25px;\">"
                        + "    <h2 style=\"color: #4f46e5; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;\">Resume<span style=\"color: #0f172a;\">Parser</span></h2>"
                        + "    <p style=\"color: #64748b; font-size: 14px; margin-top: 5px;\">Candidate Screening Portal</p>"
                        + "  </div>"
                        + "  <div style=\"border-bottom: 1px solid #f1f5f9; margin-bottom: 25px;\"></div>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 20px;\">Hello,</p>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">Use the verification code below to access your HR/Admin dashboard. This code is valid for <b>5 minutes</b>.</p>"
                        + "  <div style=\"text-align: center; margin: 30px 0;\">"
                        + "    <span style=\"display: inline-block; background-color: #f8fafc; border: 2px dashed #cbd5e1; color: #0f172a; font-size: 32px; font-weight: 700; letter-spacing: 6px; padding: 12px 30px; border-radius: 12px;\">" + otp + "</span>"
                        + "  </div>"
                        + "  <p style=\"color: #64748b; font-size: 13px; line-height: 1.5; margin-bottom: 25px;\">If you did not request this code, please ignore this email or contact support if you suspect unauthorized access.</p>"
                        + "  <div style=\"border-bottom: 1px solid #f1f5f9; margin-bottom: 20px;\"></div>"
                        + "  <p style=\"color: #94a3b8; font-size: 11px; text-align: center; margin: 0;\">This is an automated message, please do not reply directly.</p>"
                        + "</div>";

                helper.setText(htmlContent, true);
                mailSender.send(message);
                log.info("OTP email successfully sent to {}", email);
            } catch (Exception e) {
                log.error("Failed to send OTP email to {}. Error: {}", email, e.getMessage());
            }
        } else {
            log.warn("JavaMailSender bean is not configured or disabled. Skipping email sending.");
        }
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        OtpData cachedData = otpCache.get(email);
        if (cachedData == null) {
            log.warn("No OTP code cached for email: {}", email);
            return false;
        }

        // Check expiration
        if (LocalDateTime.now().isAfter(cachedData.getExpiryTime())) {
            log.warn("OTP has expired for email: {}", email);
            otpCache.remove(email);
            return false;
        }

        // Verify value
        if (cachedData.getOtpCode().equals(otp)) {
            otpCache.remove(email); // consume OTP after successful validation
            log.info("OTP successfully verified for email: {}", email);
            return true;
        }

        log.warn("OTP mismatch for email: {}", email);
        return false;
    }
}
