package com.examportal.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CandidateResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String location;
    private String linkedinProfile;
    private String professionalSummary;
    private List<Map<String, Object>> education;
    private List<Map<String, Object>> experience;
    private List<Map<String, String>> skills;
    private List<String> projects;
    private List<String> certifications;
    private List<String> languages;
    private String totalYearsExperience;
    private Integer atsScore;
    private String candidateStatus;
    private boolean shortlisted;

    // ATS feedback details
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private List<String> strengths;
    private List<String> improvements;
    private String feedbackReason;
}
