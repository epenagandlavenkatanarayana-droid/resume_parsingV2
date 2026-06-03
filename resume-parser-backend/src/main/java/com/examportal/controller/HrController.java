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

    @PutMapping("/profiles/{id}/shortlist")
    public ResponseEntity<ApiResponse> toggleShortlist(@PathVariable String id) {
        Candidate candidate = candidateRepository.findById(id).orElse(null);
        if (candidate == null) {
            return new ResponseEntity<>(new ApiResponse("Candidate not found", false), HttpStatus.NOT_FOUND);
        }
        
        boolean newShortlistState = !candidate.isShortlisted();
        candidate.setShortlisted(newShortlistState);
        
        if (newShortlistState) {
            // Toggling Shortlisted = True
            // If they are not eligible, update status to "Eligible"
            if (!"Eligible".equals(candidate.getCandidateStatus())) {
                candidate.setCandidateStatus("Eligible");
            }
            
            // Save updated main candidate details
            candidateRepository.save(candidate);
            
            // Save to eligible collection
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
                    .build();
            eligibleCandidateRepository.save(eligible);
            
            // Delete from not eligible collection
            try {
                notEligibleCandidateRepository.deleteById(id);
            } catch (Exception e) {
                // Ignore if not present
            }
            
            // Save to shortlisted candidates collection
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
                    .build();
            shortlistedCandidateRepository.save(shortlisted);
            
        } else {
            // Toggling Shortlisted = False
            // Delete from shortlisted candidates collection
            try {
                shortlistedCandidateRepository.deleteById(id);
            } catch (Exception e) {
                // Ignore if not present
            }
            
            // Revert candidate status based on original ATS score
            if (candidate.getAtsScore() != null && candidate.getAtsScore() >= 80) {
                candidate.setCandidateStatus("Eligible");
                candidateRepository.save(candidate);
                
                // Add back to eligible candidates collection
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
                        .build();
                eligibleCandidateRepository.save(eligible);
            } else {
                candidate.setCandidateStatus("Not Eligible");
                candidateRepository.save(candidate);
                
                // Remove from eligible candidates collection if they exist there
                try {
                    eligibleCandidateRepository.deleteById(id);
                } catch (Exception e) {}
                
                // Add back to not eligible candidates collection
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
                        .build();
                notEligibleCandidateRepository.save(notEligible);
            }
        }
        
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
            candidateRepository.save(candidate);
            
            // Delete from not eligible collection
            try {
                notEligibleCandidateRepository.deleteById(id);
            } catch (Exception e) {}
            
            // Save to eligible collection
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
                    .build();
            eligibleCandidateRepository.save(eligible);
            
            // If they are shortlisted, also make sure they are saved to shortlisted collection
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
                        .build();
                shortlistedCandidateRepository.save(shortlisted);
            }
        } else {
            candidate.setCandidateStatus("Not Eligible");
            // If they were shortlisted, we should remove them from shortlist since they are now Not Eligible manually
            candidate.setShortlisted(false);
            candidateRepository.save(candidate);
            
            // Delete from eligible and shortlisted collections
            try {
                eligibleCandidateRepository.deleteById(id);
            } catch (Exception e) {}
            try {
                shortlistedCandidateRepository.deleteById(id);
            } catch (Exception e) {}
            
            // Save to not eligible collection
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
                    .build();
            notEligibleCandidateRepository.save(notEligible);
        }
        
        String status = "Eligible".equals(candidate.getCandidateStatus()) ? "marked as eligible" : "marked as not eligible";
        return new ResponseEntity<>(new ApiResponse("Candidate " + status, true), HttpStatus.OK);
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
