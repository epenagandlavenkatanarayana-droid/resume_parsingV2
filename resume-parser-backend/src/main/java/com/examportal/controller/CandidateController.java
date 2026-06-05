package com.examportal.controller;

import com.examportal.entity.Candidate;
import com.examportal.service.FileService;
import com.examportal.service.ResumeParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/candidate")
public class CandidateController {

    private static final Logger log = LoggerFactory.getLogger(CandidateController.class);

    @Autowired
    private FileService fileService;

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.examportal.repository.CandidateRepository candidateRepository;

    @Autowired
    private com.examportal.repository.EligibleCandidateRepository eligibleCandidateRepository;

    @Autowired
    private com.examportal.repository.NotEligibleCandidateRepository notEligibleCandidateRepository;

    @Autowired
    private com.examportal.repository.ShortlistedCandidateRepository shortlistedCandidateRepository;

    @PostMapping("/upload-resume")
    public ResponseEntity<Map<String, Object>> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "jobDescription", required = false) String jobDescription) {

        try {
            log.info("Upload request received: file={}, fullName={}, email={}", 
                file.getOriginalFilename(), fullName, email);

            fileService.uploadResume(file);
            Candidate candidate = resumeParserService.parseAndSaveResume(file, fullName, email, phone, jobDescription);

            Map<String, Object> response = new HashMap<>();
            response.put("FullName", candidate.getFullName());
            response.put("Email", candidate.getEmail());
            response.put("PhoneNumber", candidate.getPhoneNumber());
            response.put("Location", candidate.getLocation());
            response.put("ATSScore", candidate.getAtsScore());
            response.put("Status", candidate.getCandidateStatus());
            
            String storedIn = candidate.getAtsScore() >= 80 ? "Eligible_Candidates" : "Not_Eligible_Candidates";
            response.put("StoredIn", storedIn);

            try {
                response.put("MatchingSkills", candidate.getMatchingSkills() != null && !candidate.getMatchingSkills().isEmpty() 
                    ? objectMapper.readValue(candidate.getMatchingSkills(), java.util.List.class) : new java.util.ArrayList<>());
                response.put("MissingSkills", candidate.getMissingSkills() != null && !candidate.getMissingSkills().isEmpty() 
                    ? objectMapper.readValue(candidate.getMissingSkills(), java.util.List.class) : new java.util.ArrayList<>());
                response.put("Strengths", candidate.getStrengths() != null && !candidate.getStrengths().isEmpty() 
                    ? objectMapper.readValue(candidate.getStrengths(), java.util.List.class) : new java.util.ArrayList<>());
                response.put("Improvements", candidate.getImprovements() != null && !candidate.getImprovements().isEmpty() 
                    ? objectMapper.readValue(candidate.getImprovements(), java.util.List.class) : new java.util.ArrayList<>());
            } catch (Exception e) {
                response.put("MatchingSkills", new java.util.ArrayList<>());
                response.put("MissingSkills", new java.util.ArrayList<>());
                response.put("Strengths", new java.util.ArrayList<>());
                response.put("Improvements", new java.util.ArrayList<>());
            }
            response.put("FeedbackReason", candidate.getFeedbackReason());

            log.info("Resume uploaded and parsed successfully for candidate: {}", candidate.getEmail());
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.warn("Validation failed for resume upload: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            errorResponse.put("success", false);
            return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("Error uploading resume: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Upload failed: " + e.getMessage());
            errorResponse.put("success", false);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/parse-resume", produces = "application/json")
    public ResponseEntity<String> parseResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobDescription", required = false) String jobDescription) {
        try {
            String jsonResult = resumeParserService.parseResume(file, jobDescription);
            return new ResponseEntity<>(jsonResult, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error parsing resume: {}", e.getMessage(), e);
            return new ResponseEntity<>("{\"error\": \"" + e.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void syncCandidateAcrossCollections(Candidate candidate) {
        candidateRepository.save(candidate);

        if ("Eligible".equals(candidate.getCandidateStatus())) {
            com.examportal.entity.EligibleCandidate eligible = com.examportal.entity.EligibleCandidate.builder()
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
            com.examportal.entity.NotEligibleCandidate notEligible = com.examportal.entity.NotEligibleCandidate.builder()
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
            com.examportal.entity.ShortlistedCandidate shortlisted = com.examportal.entity.ShortlistedCandidate.builder()
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

    @GetMapping("/offer-status")
    public ResponseEntity<?> getOfferStatus(@RequestParam("email") String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Email is required", "success", false), HttpStatus.BAD_REQUEST);
        }
        java.util.Optional<Candidate> optionalCandidate = candidateRepository.findByEmail(email);
        if (optionalCandidate.isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Candidate not found", "success", false), HttpStatus.NOT_FOUND);
        }
        Candidate c = optionalCandidate.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", c.getId());
        response.put("fullName", c.getFullName());
        response.put("email", c.getEmail());
        response.put("phoneNumber", c.getPhoneNumber());
        response.put("recruitmentStage", c.getRecruitmentStage());
        response.put("designation", c.getDesignation());
        response.put("salaryPackage", c.getSalaryPackage());
        response.put("joiningDate", c.getJoiningDate());
        response.put("companyPolicies", c.getCompanyPolicies());
        response.put("atsScore", c.getAtsScore());
        response.put("candidateStatus", c.getCandidateStatus());
        response.put("shortlisted", c.isShortlisted());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/offer-status/accept")
    public ResponseEntity<?> acceptOffer(@RequestParam("email") String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Email is required", "success", false), HttpStatus.BAD_REQUEST);
        }
        java.util.Optional<Candidate> optionalCandidate = candidateRepository.findByEmail(email);
        if (optionalCandidate.isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Candidate not found", "success", false), HttpStatus.NOT_FOUND);
        }
        Candidate candidate = optionalCandidate.get();
        candidate.setRecruitmentStage("HIRED");
        syncCandidateAcrossCollections(candidate);
        
        return new ResponseEntity<>(Map.of("message", "Offer accepted successfully!", "success", true), HttpStatus.OK);
    }
}
