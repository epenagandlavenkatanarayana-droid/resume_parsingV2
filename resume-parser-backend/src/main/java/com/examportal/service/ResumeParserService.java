package com.examportal.service;

import com.examportal.entity.Candidate;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeParserService {
    /**
     * Parses the given resume file and returns a JSON string 
     * containing structured information using AI, matching against the job description.
     */
    String parseResume(MultipartFile file, String jobDescription);

    /**
     * Parses the given resume file using AI and saves the extracted
     * structured data into the database tables (Candidates, Eligible/NotEligible).
     */
    Candidate parseAndSaveResume(MultipartFile file, String manualFullName, String manualEmail, String manualPhone, String jobDescription);
}
