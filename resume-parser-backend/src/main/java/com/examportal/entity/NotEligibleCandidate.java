package com.examportal.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotEligibleCandidate {

    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String location;
    private String linkedinProfile;
    private String professionalSummary;
    private String educationDetails;
    private String experienceDetails;
    private String skills;
    private String certifications;
    private String projects;
    private String languages;
    private String totalYearsExperience;
    private Integer atsScore;
    private String candidateStatus;
    @Builder.Default
    private boolean shortlisted = false;
    private String resumeHash;
    private String jobDescription;
    @Builder.Default
    private String resumeUploadDate = java.time.LocalDateTime.now().toString();

    @Builder.Default
    private String recruitmentStage = "APPLICATION_SUBMITTED";
    private String designation;
    private String salaryPackage;
    private String joiningDate;
    private String companyPolicies;

    // ATS feedback details
    private String matchingSkills;
    private String missingSkills;
    private String strengths;
    private String improvements;
    private String feedbackReason;
}
