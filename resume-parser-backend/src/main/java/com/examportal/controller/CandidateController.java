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
}
