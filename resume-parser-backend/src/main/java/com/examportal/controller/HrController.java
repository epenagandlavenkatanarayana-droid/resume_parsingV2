package com.examportal.controller;

import com.examportal.entity.Candidate;
import com.examportal.dto.CandidateResponse;
import com.examportal.payload.ApiResponse;
import com.examportal.repository.CandidateRepository;
import com.examportal.entity.User;
import com.examportal.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.examportal.entity.EligibleCandidate;
import com.examportal.entity.NotEligibleCandidate;
import com.examportal.entity.ShortlistedCandidate;
import com.examportal.repository.EligibleCandidateRepository;
import com.examportal.repository.NotEligibleCandidateRepository;
import com.examportal.repository.ShortlistedCandidateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
public class HrController {

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private EligibleCandidateRepository eligibleCandidateRepository;

    @Autowired
    private NotEligibleCandidateRepository notEligibleCandidateRepository;

    @Autowired
    private ShortlistedCandidateRepository shortlistedCandidateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @GetMapping("/profiles")
    public ResponseEntity<List<CandidateResponse>> getAllProfiles() {
        List<Candidate> candidates = candidateRepository.findAll();
        List<CandidateResponse> responseList = new ArrayList<>();
        
        for (Candidate c : candidates) {
            CandidateResponse res = new CandidateResponse();
            res.setId(c.getId());
            res.setFullName(c.getFullName());
            res.setEmail(c.getEmail());
            res.setPhone(c.getPhoneNumber());
            res.setLocation(c.getLocation());
            res.setLinkedinProfile(c.getLinkedinProfile());
            res.setProfessionalSummary(c.getProfessionalSummary());
            res.setTotalYearsExperience(c.getTotalYearsExperience());
            res.setAtsScore(c.getAtsScore());
            res.setCandidateStatus(c.getCandidateStatus());
            res.setShortlisted(c.isShortlisted());
            res.setRecruitmentStage(c.getRecruitmentStage());
            res.setDesignation(c.getDesignation());
            res.setSalaryPackage(c.getSalaryPackage());
            res.setJoiningDate(c.getJoiningDate());
            res.setCompanyPolicies(c.getCompanyPolicies());
            
            // Map Matching Skills
            try {
                if (c.getMatchingSkills() != null && !c.getMatchingSkills().isEmpty()) {
                    res.setMatchingSkills(objectMapper.readValue(c.getMatchingSkills(), new TypeReference<List<String>>() {}));
                } else {
                    res.setMatchingSkills(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setMatchingSkills(new ArrayList<>());
            }

            // Map Missing Skills
            try {
                if (c.getMissingSkills() != null && !c.getMissingSkills().isEmpty()) {
                    res.setMissingSkills(objectMapper.readValue(c.getMissingSkills(), new TypeReference<List<String>>() {}));
                } else {
                    res.setMissingSkills(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setMissingSkills(new ArrayList<>());
            }

            // Map Strengths
            try {
                if (c.getStrengths() != null && !c.getStrengths().isEmpty()) {
                    res.setStrengths(objectMapper.readValue(c.getStrengths(), new TypeReference<List<String>>() {}));
                } else {
                    res.setStrengths(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setStrengths(new ArrayList<>());
            }

            // Map Improvements
            try {
                if (c.getImprovements() != null && !c.getImprovements().isEmpty()) {
                    res.setImprovements(objectMapper.readValue(c.getImprovements(), new TypeReference<List<String>>() {}));
                } else {
                    res.setImprovements(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setImprovements(new ArrayList<>());
            }

            res.setFeedbackReason(c.getFeedbackReason());
            
            // 1. Map Education Details (remap graduation_year to graduationYear)
            try {
                if (c.getEducationDetails() != null && !c.getEducationDetails().isEmpty()) {
                    List<Map<String, Object>> rawEduList = objectMapper.readValue(
                            c.getEducationDetails(), 
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                    List<Map<String, Object>> formattedEduList = new ArrayList<>();
                    for (Map<String, Object> rawEdu : rawEduList) {
                        Map<String, Object> formattedEdu = new HashMap<>(rawEdu);
                        if (rawEdu.containsKey("graduation_year")) {
                            formattedEdu.put("graduationYear", rawEdu.get("graduation_year"));
                        }
                        formattedEduList.add(formattedEdu);
                    }
                    res.setEducation(formattedEduList);
                } else {
                    res.setEducation(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setEducation(new ArrayList<>());
            }

            // 2. Map Experience Details (remap job_title to jobTitle, start_date to startDate, end_date to endDate)
            try {
                if (c.getExperienceDetails() != null && !c.getExperienceDetails().isEmpty()) {
                    List<Map<String, Object>> rawExpList = objectMapper.readValue(
                            c.getExperienceDetails(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                    List<Map<String, Object>> formattedExpList = new ArrayList<>();
                    for (Map<String, Object> rawExp : rawExpList) {
                        Map<String, Object> formattedExp = new HashMap<>(rawExp);
                        if (rawExp.containsKey("job_title")) {
                            formattedExp.put("jobTitle", rawExp.get("job_title"));
                        }
                        if (rawExp.containsKey("start_date")) {
                            formattedExp.put("startDate", rawExp.get("start_date"));
                        }
                        if (rawExp.containsKey("end_date")) {
                            formattedExp.put("endDate", rawExp.get("end_date"));
                        }
                        formattedExpList.add(formattedExp);
                    }
                    res.setExperience(formattedExpList);
                } else {
                    res.setExperience(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setExperience(new ArrayList<>());
            }
                // duplicate block removed

            // 3. Map Skills list of objects
            try {
                if (c.getSkills() != null && !c.getSkills().isEmpty()) {
                    List<String> rawSkillsList = objectMapper.readValue(
                            c.getSkills(), 
                            new TypeReference<List<String>>() {}
                    );
                    List<Map<String, String>> skillsObjList = new ArrayList<>();
                    for (String skillName : rawSkillsList) {
                        Map<String, String> skillObj = new HashMap<>();
                        skillObj.put("skillName", skillName);
                        skillsObjList.add(skillObj);
                    }
                    res.setSkills(skillsObjList);
                } else {
                    res.setSkills(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setSkills(new ArrayList<>());
            }

            // 4. Map Projects list
            try {
                if (c.getProjects() != null && !c.getProjects().isEmpty()) {
                    List<String> projectsList = objectMapper.readValue(
                            c.getProjects(), 
                            new TypeReference<List<String>>() {}
                    );
                    res.setProjects(projectsList);
                } else {
                    res.setProjects(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setProjects(new ArrayList<>());
            }

            // 5. Map Certifications list
            try {
                if (c.getCertifications() != null && !c.getCertifications().isEmpty()) {
                    List<String> certificationsList = objectMapper.readValue(
                            c.getCertifications(), 
                            new TypeReference<List<String>>() {}
                    );
                    res.setCertifications(certificationsList);
                } else {
                    res.setCertifications(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setCertifications(new ArrayList<>());
            }

            // 6. Map Languages list
            try {
                if (c.getLanguages() != null && !c.getLanguages().isEmpty()) {
                    List<String> languagesList = objectMapper.readValue(
                            c.getLanguages(), 
                            new TypeReference<List<String>>() {}
                    );
                    res.setLanguages(languagesList);
                } else {
                    res.setLanguages(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setLanguages(new ArrayList<>());
            }

            responseList.add(res);
        }
        
        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HrController.class);

    private void syncCandidateAcrossCollections(Candidate candidate) {
        candidateRepository.save(candidate);

        if ("Eligible".equals(candidate.getCandidateStatus())) {
            EligibleCandidate eligible = EligibleCandidate.builder()
                    .id(candidate.getId())
                    .fullName(candidate.getFullName())
                    .email(candidate.getEmail())
                    .phoneNumber(candidate.getPhoneNumber())
                    .location(candidate.getLocation())
                    .linkedinProfile(candidate.getLinkedinProfile())
                    .professionalSummary(candidate.getProfessionalSummary())
                    .educationDetails(candidate.getEducationDetails())
                    .experienceDetails(candidate.getExperienceDetails())
                    .skills(candidate.getSkills())
                    .certifications(candidate.getCertifications())
                    .projects(candidate.getProjects())
                    .languages(candidate.getLanguages())
                    .totalYearsExperience(candidate.getTotalYearsExperience())
                    .atsScore(candidate.getAtsScore())
                    .candidateStatus(candidate.getCandidateStatus())
                    .shortlisted(candidate.isShortlisted())
                    .resumeHash(candidate.getResumeHash())
                    .jobDescription(candidate.getJobDescription())
                    .resumeUploadDate(candidate.getResumeUploadDate())
                    .matchingSkills(candidate.getMatchingSkills())
                    .missingSkills(candidate.getMissingSkills())
                    .strengths(candidate.getStrengths())
                    .improvements(candidate.getImprovements())
                    .feedbackReason(candidate.getFeedbackReason())
                    .recruitmentStage(candidate.getRecruitmentStage())
                    .designation(candidate.getDesignation())
                    .salaryPackage(candidate.getSalaryPackage())
                    .joiningDate(candidate.getJoiningDate())
                    .companyPolicies(candidate.getCompanyPolicies())
                    .build();
            eligibleCandidateRepository.save(eligible);

            try {
                notEligibleCandidateRepository.deleteById(candidate.getId());
            } catch (Exception e) {}
        } else {
            NotEligibleCandidate notEligible = NotEligibleCandidate.builder()
                    .id(candidate.getId())
                    .fullName(candidate.getFullName())
                    .email(candidate.getEmail())
                    .phoneNumber(candidate.getPhoneNumber())
                    .location(candidate.getLocation())
                    .linkedinProfile(candidate.getLinkedinProfile())
                    .professionalSummary(candidate.getProfessionalSummary())
                    .educationDetails(candidate.getEducationDetails())
                    .experienceDetails(candidate.getExperienceDetails())
                    .skills(candidate.getSkills())
                    .certifications(candidate.getCertifications())
                    .projects(candidate.getProjects())
                    .languages(candidate.getLanguages())
                    .totalYearsExperience(candidate.getTotalYearsExperience())
                    .atsScore(candidate.getAtsScore())
                    .candidateStatus(candidate.getCandidateStatus())
                    .shortlisted(candidate.isShortlisted())
                    .resumeHash(candidate.getResumeHash())
                    .jobDescription(candidate.getJobDescription())
                    .resumeUploadDate(candidate.getResumeUploadDate())
                    .matchingSkills(candidate.getMatchingSkills())
                    .missingSkills(candidate.getMissingSkills())
                    .strengths(candidate.getStrengths())
                    .improvements(candidate.getImprovements())
                    .feedbackReason(candidate.getFeedbackReason())
                    .recruitmentStage(candidate.getRecruitmentStage())
                    .designation(candidate.getDesignation())
                    .salaryPackage(candidate.getSalaryPackage())
                    .joiningDate(candidate.getJoiningDate())
                    .companyPolicies(candidate.getCompanyPolicies())
                    .build();
            notEligibleCandidateRepository.save(notEligible);

            try {
                eligibleCandidateRepository.deleteById(candidate.getId());
            } catch (Exception e) {}
        }

        if (candidate.isShortlisted()) {
            ShortlistedCandidate shortlisted = ShortlistedCandidate.builder()
                    .id(candidate.getId())
                    .fullName(candidate.getFullName())
                    .email(candidate.getEmail())
                    .phoneNumber(candidate.getPhoneNumber())
                    .location(candidate.getLocation())
                    .linkedinProfile(candidate.getLinkedinProfile())
                    .professionalSummary(candidate.getProfessionalSummary())
                    .educationDetails(candidate.getEducationDetails())
                    .experienceDetails(candidate.getExperienceDetails())
                    .skills(candidate.getSkills())
                    .certifications(candidate.getCertifications())
                    .projects(candidate.getProjects())
                    .languages(candidate.getLanguages())
                    .totalYearsExperience(candidate.getTotalYearsExperience())
                    .atsScore(candidate.getAtsScore())
                    .candidateStatus(candidate.getCandidateStatus())
                    .shortlisted(candidate.isShortlisted())
                    .resumeHash(candidate.getResumeHash())
                    .jobDescription(candidate.getJobDescription())
                    .resumeUploadDate(candidate.getResumeUploadDate())
                    .matchingSkills(candidate.getMatchingSkills())
                    .missingSkills(candidate.getMissingSkills())
                    .strengths(candidate.getStrengths())
                    .improvements(candidate.getImprovements())
                    .feedbackReason(candidate.getFeedbackReason())
                    .recruitmentStage(candidate.getRecruitmentStage())
                    .designation(candidate.getDesignation())
                    .salaryPackage(candidate.getSalaryPackage())
                    .joiningDate(candidate.getJoiningDate())
                    .companyPolicies(candidate.getCompanyPolicies())
                    .build();
            shortlistedCandidateRepository.save(shortlisted);
        } else {
            try {
                shortlistedCandidateRepository.deleteById(candidate.getId());
            } catch (Exception e) {}
        }
    }

    private void sendBgvEmail(Candidate candidate) {
        log.info("\n==================================================" +
                 "\n=== [BGV SERVICE] Background Verification Initiated for: {}" +
                 "\n==================================================", candidate.getEmail());
        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(candidate.getEmail());
                helper.setSubject("ResumeParser - Background Verification In Progress");
                
                String htmlContent = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 500px; margin: 0 auto; padding: 30px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);\">"
                        + "  <div style=\"text-align: center; margin-bottom: 25px;\">"
                        + "    <h2 style=\"color: #4f46e5; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;\">Resume<span style=\"color: #0f172a;\">Parser</span></h2>"
                        + "    <p style=\"color: #64748b; font-size: 14px; margin-top: 5px;\">Candidate Selection Pipeline</p>"
                        + "  </div>"
                        + "  <div style=\"border-bottom: 1px solid #f1f5f9; margin-bottom: 25px;\"></div>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 20px;\">Dear " + candidate.getFullName() + ",</p>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">We are pleased to inform you that you have successfully completed the initial recruitment stages and have been shortlisted. Your <b>Background Verification (BGV)</b> process has now been initiated.</p>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">During this stage, HR and the verification team will coordinate internally to validate your educational qualifications, employment history, identity, and other required details.</p>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">No action is required from your end at this moment. We will reach out to you if we need any additional documents or clarifications. You will be notified as soon as the verification is complete.</p>"
                        + "  <div style=\"border-bottom: 1px solid #f1f5f9; margin-bottom: 20px;\"></div>"
                        + "  <p style=\"color: #94a3b8; font-size: 11px; text-align: center; margin: 0;\">This is an automated message, please do not reply directly.</p>"
                        + "</div>";

                helper.setText(htmlContent, true);
                mailSender.send(message);
                log.info("BGV email successfully sent to {}", candidate.getEmail());
            } catch (Exception e) {
                log.error("Failed to send BGV email to {}. Error: {}", candidate.getEmail(), e.getMessage());
            }
        } else {
            log.warn("JavaMailSender bean is not configured. Skipping email sending.");
        }
    }

    private void sendOfferEmail(Candidate candidate) {
        log.info("\n==================================================" +
                 "\n=== [OFFER SERVICE] Offer Letter Sent to: {}" +
                 "\n=== Designation: {}" +
                 "\n=== Salary: {}" +
                 "\n==================================================", candidate.getEmail(), candidate.getDesignation(), candidate.getSalaryPackage());
        if (mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(candidate.getEmail());
                helper.setSubject("ResumeParser - Employment Offer Letter");
                
                String acceptLink = "http://localhost:5173/offer-status?email=" + java.net.URLEncoder.encode(candidate.getEmail(), "UTF-8");
                
                String htmlContent = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 500px; margin: 0 auto; padding: 30px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);\">"
                        + "  <div style=\"text-align: center; margin-bottom: 25px;\">"
                        + "    <h2 style=\"color: #4f46e5; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;\">Resume<span style=\"color: #0f172a;\">Parser</span></h2>"
                        + "    <p style=\"color: #64748b; font-size: 14px; margin-top: 5px;\">Official Offer of Employment</p>"
                        + "  </div>"
                        + "  <div style=\"border-bottom: 1px solid #f1f5f9; margin-bottom: 25px;\"></div>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 20px;\">Dear " + candidate.getFullName() + ",</p>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">Following the successful completion of your Background Verification (BGV), we are absolutely thrilled to offer you the position of <b>" + candidate.getDesignation() + "</b> at ResumeParser!</p>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 15px;\">Below are the details of your offer:</p>"
                        + "  <div style=\"background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 25px;\">"
                        + "    <table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">"
                        + "      <tr>"
                        + "        <td style=\"color: #64748b; padding: 6px 0; font-weight: 500;\">Designation:</td>"
                        + "        <td style=\"color: #0f172a; padding: 6px 0; font-weight: 600; text-align: right;\">" + candidate.getDesignation() + "</td>"
                        + "      </tr>"
                        + "      <tr>"
                        + "        <td style=\"color: #64748b; padding: 6px 0; font-weight: 500;\">Salary Package:</td>"
                        + "        <td style=\"color: #0f172a; padding: 6px 0; font-weight: 600; text-align: right;\">" + candidate.getSalaryPackage() + "</td>"
                        + "      </tr>"
                        + "      <tr>"
                        + "        <td style=\"color: #64748b; padding: 6px 0; font-weight: 500;\">Joining Date:</td>"
                        + "        <td style=\"color: #0f172a; padding: 6px 0; font-weight: 600; text-align: right;\">" + candidate.getJoiningDate() + "</td>"
                        + "      </tr>"
                        + "    </table>"
                        + "  </div>"
                        + "  <p style=\"color: #334155; font-size: 16px; line-height: 1.6; margin-bottom: 25px;\">Please click the button below to review your complete offer letter, read the company policies, and accept your offer online:</p>"
                        + "  <div style=\"text-align: center; margin: 30px 0;\">"
                        + "    <a href=\"" + acceptLink + "\" style=\"display: inline-block; background-color: #4f46e5; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 600; padding: 12px 30px; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(79, 70, 229, 0.2);\">Review & Accept Offer</a>"
                        + "  </div>"
                        + "  <p style=\"color: #64748b; font-size: 13px; line-height: 1.5; margin-bottom: 25px;\">We look forward to welcoming you to the team!</p>"
                        + "  <div style=\"border-bottom: 1px solid #f1f5f9; margin-bottom: 20px;\"></div>"
                        + "  <p style=\"color: #94a3b8; font-size: 11px; text-align: center; margin: 0;\">This is an automated message, please do not reply directly.</p>"
                        + "</div>";

                helper.setText(htmlContent, true);
                mailSender.send(message);
                log.info("Offer letter email successfully sent to {}", candidate.getEmail());
            } catch (Exception e) {
                log.error("Failed to send Offer email to {}. Error: {}", candidate.getEmail(), e.getMessage());
            }
        } else {
            log.warn("JavaMailSender bean is not configured. Skipping email sending.");
        }
    }

    @PutMapping("/profiles/{id}/shortlist")
    public ResponseEntity<ApiResponse> toggleShortlist(@PathVariable String id) {
        Candidate candidate = candidateRepository.findById(id).orElse(null);
        if (candidate == null) {
            return new ResponseEntity<>(new ApiResponse("Candidate not found", false), HttpStatus.NOT_FOUND);
        }
        
        boolean newShortlistState = !candidate.isShortlisted();
        candidate.setShortlisted(newShortlistState);
        
        if (newShortlistState) {
            if (!"Eligible".equals(candidate.getCandidateStatus())) {
                candidate.setCandidateStatus("Eligible");
            }
            candidate.setRecruitmentStage("SHORTLISTED");
        } else {
            if (candidate.getAtsScore() != null && candidate.getAtsScore() >= 80) {
                candidate.setCandidateStatus("Eligible");
            } else {
                candidate.setCandidateStatus("Not Eligible");
            }
            candidate.setRecruitmentStage("APPLICATION_SUBMITTED");
        }
        
        syncCandidateAcrossCollections(candidate);
        
        String status = candidate.isShortlisted() ? "shortlisted" : "removed from shortlist";
        return new ResponseEntity<>(new ApiResponse("Candidate " + status, true), HttpStatus.OK);
    }

    @PutMapping("/profiles/{id}/eligible")
    public ResponseEntity<ApiResponse> toggleEligibility(@PathVariable String id) {
        Candidate candidate = candidateRepository.findById(id).orElse(null);
        if (candidate == null) {
            return new ResponseEntity<>(new ApiResponse("Candidate not found", false), HttpStatus.NOT_FOUND);
        }
        
        boolean newEligibility = !"Eligible".equals(candidate.getCandidateStatus());
        
        if (newEligibility) {
            candidate.setCandidateStatus("Eligible");
        } else {
            candidate.setCandidateStatus("Not Eligible");
            candidate.setShortlisted(false);
            candidate.setRecruitmentStage("APPLICATION_SUBMITTED");
        }
        
        syncCandidateAcrossCollections(candidate);
        
        String status = "Eligible".equals(candidate.getCandidateStatus()) ? "marked as eligible" : "marked as not eligible";
        return new ResponseEntity<>(new ApiResponse("Candidate " + status, true), HttpStatus.OK);
    }

    @PutMapping("/profiles/{id}/stage")
    public ResponseEntity<ApiResponse> updateStage(@PathVariable String id, @RequestBody Map<String, String> request) {
        Candidate candidate = candidateRepository.findById(id).orElse(null);
        if (candidate == null) {
            return new ResponseEntity<>(new ApiResponse("Candidate not found", false), HttpStatus.NOT_FOUND);
        }
        
        String stage = request.get("stage");
        if (stage == null || stage.trim().isEmpty()) {
            return new ResponseEntity<>(new ApiResponse("Stage is required", false), HttpStatus.BAD_REQUEST);
        }
        
        candidate.setRecruitmentStage(stage);
        syncCandidateAcrossCollections(candidate);
        
        if ("BGV_INITIATED".equalsIgnoreCase(stage)) {
            sendBgvEmail(candidate);
        }
        
        return new ResponseEntity<>(new ApiResponse("Candidate recruitment stage updated to " + stage, true), HttpStatus.OK);
    }

    @PostMapping("/profiles/{id}/send-offer")
    public ResponseEntity<ApiResponse> sendOffer(@PathVariable String id, @RequestBody Map<String, String> request) {
        Candidate candidate = candidateRepository.findById(id).orElse(null);
        if (candidate == null) {
            return new ResponseEntity<>(new ApiResponse("Candidate not found", false), HttpStatus.NOT_FOUND);
        }
        
        String designation = request.get("designation");
        String salaryPackage = request.get("salaryPackage");
        String joiningDate = request.get("joiningDate");
        String companyPolicies = request.get("companyPolicies");
        
        candidate.setDesignation(designation);
        candidate.setSalaryPackage(salaryPackage);
        candidate.setJoiningDate(joiningDate);
        candidate.setCompanyPolicies(companyPolicies);
        candidate.setRecruitmentStage("OFFER_SENT");
        
        syncCandidateAcrossCollections(candidate);
        
        sendOfferEmail(candidate);
        
        return new ResponseEntity<>(new ApiResponse("Offer sent successfully to " + candidate.getEmail(), true), HttpStatus.OK);
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(
            java.security.Principal principal,
            @RequestBody Map<String, String> request) {
        
        if (principal == null) {
            return new ResponseEntity<>(new ApiResponse("Unauthorized", false), HttpStatus.UNAUTHORIZED);
        }
        
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new ResponseEntity<>(new ApiResponse("User not found", false), HttpStatus.NOT_FOUND);
        }
        
        String fullName = request.get("fullName");
        String phoneNumber = request.get("phoneNumber");
        String password = request.get("password");
        
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber.trim());
        }
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password.trim()));
        }
        
        userRepository.save(user);
        
        return new ResponseEntity<>(new ApiResponse("Profile updated successfully", true), HttpStatus.OK);
    }
}
