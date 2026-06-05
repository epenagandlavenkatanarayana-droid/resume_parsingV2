package com.examportal.serviceImpl;

import com.examportal.service.ResumeParserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.examportal.entity.Candidate;
import com.examportal.entity.EligibleCandidate;
import com.examportal.entity.NotEligibleCandidate;
import com.examportal.entity.ShortlistedCandidate;
import com.examportal.repository.CandidateRepository;
import com.examportal.repository.EligibleCandidateRepository;
import com.examportal.repository.NotEligibleCandidateRepository;
import com.examportal.repository.ShortlistedCandidateRepository;

@Service
public class ResumeParserServiceImpl implements ResumeParserService {

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final CandidateRepository candidateRepository;
    private final EligibleCandidateRepository eligibleCandidateRepository;
    private final NotEligibleCandidateRepository notEligibleCandidateRepository;
    private final ShortlistedCandidateRepository shortlistedCandidateRepository;
    private final ObjectMapper objectMapper;

    public ResumeParserServiceImpl(WebClient.Builder webClientBuilder, 
                                   CandidateRepository candidateRepository, 
                                   EligibleCandidateRepository eligibleCandidateRepository, 
                                   NotEligibleCandidateRepository notEligibleCandidateRepository, 
                                   ShortlistedCandidateRepository shortlistedCandidateRepository,
                                   ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.candidateRepository = candidateRepository;
        this.eligibleCandidateRepository = eligibleCandidateRepository;
        this.notEligibleCandidateRepository = notEligibleCandidateRepository;
        this.shortlistedCandidateRepository = shortlistedCandidateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String parseResume(MultipartFile file, String jobDescription) {
        try {
            String extractedText = extractText(file);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new RuntimeException("Could not extract text from the file. It might be empty or in an unsupported format.");
            }

            return callGeminiAPI(extractedText, jobDescription);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing resume: " + e.getMessage(), e);
        }
    }

    @Override
    public Candidate parseAndSaveResume(MultipartFile file, String manualFullName, String manualEmail, String manualPhone, String jobDescription) {
        try {
            String extractedText = extractText(file);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new RuntimeException("Could not extract text from the file. It might be empty or in an unsupported format.");
            }

            String resumeHash = calculateSHA256(extractedText);
            
            // Look up for duplicate resume in database with matching job description
            List<Candidate> existingWithHash = candidateRepository.findByResumeHash(resumeHash);
            String targetJd = jobDescription == null ? "" : jobDescription.trim();
            Candidate cachedMatch = null;
            for (Candidate c : existingWithHash) {
                String candidateJd = c.getJobDescription() == null ? "" : c.getJobDescription().trim();
                if (candidateJd.equalsIgnoreCase(targetJd)) {
                    cachedMatch = c;
                    break;
                }
            }

            String jsonResult = callGeminiAPI(extractedText, jobDescription);
            JsonNode rootNode = objectMapper.readTree(jsonResult);
            
            // Prefer manually entered details over AI parsed ones
            String fullName = manualFullName != null && !manualFullName.isEmpty() ? manualFullName : rootNode.path("full_name").asText(null);
            String email = manualEmail != null && !manualEmail.isEmpty() ? manualEmail : rootNode.path("email").asText(null);
            String phone = manualPhone != null && !manualPhone.isEmpty() ? manualPhone : rootNode.path("phone").asText(null);
            
            if (manualFullName != null && !manualFullName.isEmpty()) {
                if (!manualFullName.matches("^[a-zA-Z\\s]+$")) {
                    throw new IllegalArgumentException("Full Name must contain only letters and spaces.");
                }
            }

            if (manualPhone != null && !manualPhone.isEmpty()) {
                if (!manualPhone.matches("^\\d{10}$")) {
                    throw new IllegalArgumentException("Phone number must be exactly 10 digits.");
                }
            }

            Optional<Candidate> existingOpt = Optional.empty();
            if (email != null && !email.isEmpty()) {
                existingOpt = candidateRepository.findByEmail(email);
            }

            String existingId = null;
            String oldStatus = null;
            boolean wasShortlisted = false;
            Candidate existingCand = null;

            if (existingOpt.isPresent()) {
                existingCand = existingOpt.get();
                existingId = existingCand.getId();
                oldStatus = existingCand.getCandidateStatus();
                wasShortlisted = existingCand.isShortlisted();
            }

            Candidate candidate = new Candidate();
            if (existingId != null) {
                candidate.setId(existingId);
            }

            candidate.setFullName(fullName);
            candidate.setEmail(email);
            candidate.setPhoneNumber(phone);
            
            candidate.setLocation(rootNode.path("location").asText(null));
            candidate.setLinkedinProfile(rootNode.path("linkedin").asText(null));
            candidate.setProfessionalSummary(rootNode.path("professional_summary").asText(null));
            
            // Serialize nested JSON arrays/objects to DB text columns
            String educationJson = objectMapper.writeValueAsString(rootNode.path("education_details"));
            String experienceJson = objectMapper.writeValueAsString(rootNode.path("experience_details"));
            String skillsJson = objectMapper.writeValueAsString(rootNode.path("skills"));
            String certificationsJson = objectMapper.writeValueAsString(rootNode.path("certifications"));
            String projectsJson = objectMapper.writeValueAsString(rootNode.path("projects"));
            String languagesJson = objectMapper.writeValueAsString(rootNode.path("languages"));

            candidate.setEducationDetails(educationJson);
            candidate.setExperienceDetails(experienceJson);
            candidate.setSkills(skillsJson);
            candidate.setCertifications(certificationsJson);
            candidate.setProjects(projectsJson);
            candidate.setLanguages(languagesJson);
            
            candidate.setTotalYearsExperience(rootNode.path("total_experience").asText(null));
            candidate.setResumeHash(resumeHash);
            candidate.setJobDescription(jobDescription);

            int atsScore;
            if (cachedMatch != null) {
                atsScore = cachedMatch.getAtsScore();
                System.out.println("====== [ATS CACHE RESUME HASH MATCH] Reusing score: " + atsScore + " ======");
            } else {
                atsScore = calculateDeterministicAtsScore(
                        extractedText, 
                        jobDescription, 
                        candidate.getTotalYearsExperience(), 
                        educationJson, 
                        certificationsJson, 
                        projectsJson, 
                        languagesJson, 
                        fullName, 
                        email, 
                        phone
                );
                System.out.println("====== [ATS CALCULATION DETERMINISTIC] Computed score: " + atsScore + " ======");
            }

            candidate.setAtsScore(atsScore);
            String status = atsScore >= 80 ? "Eligible" : "Not Eligible";
            candidate.setCandidateStatus(status);
            
            // Set stage
            String initialStage = atsScore >= 80 ? "RESUME_SCREENING" : "APPLICATION_SUBMITTED";
            candidate.setRecruitmentStage(existingCand != null ? existingCand.getRecruitmentStage() : initialStage);
            if (atsScore < 80) {
                candidate.setRecruitmentStage("APPLICATION_SUBMITTED");
            }
            
            boolean newShortlisted = false;
            if (atsScore >= 80) {
                newShortlisted = wasShortlisted;
            }
            candidate.setShortlisted(newShortlisted);

            // Preserve offer details if they existed
            if (existingCand != null) {
                candidate.setDesignation(existingCand.getDesignation());
                candidate.setSalaryPackage(existingCand.getSalaryPackage());
                candidate.setJoiningDate(existingCand.getJoiningDate());
                candidate.setCompanyPolicies(existingCand.getCompanyPolicies());
            }
            
            // Populate dynamic ATS feedback
            populateAtsFeedback(candidate, extractedText);
            
            // If candidate already exists, clean up from their previous status collections
            if (existingId != null) {
                if ("Eligible".equals(oldStatus)) {
                    try {
                        eligibleCandidateRepository.deleteById(existingId);
                    } catch (Exception e) {
                        // ignore
                    }
                } else if ("Not Eligible".equals(oldStatus)) {
                    try {
                        notEligibleCandidateRepository.deleteById(existingId);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                
                // If they were shortlisted but are no longer eligible, remove from shortlisted
                if (wasShortlisted && !"Eligible".equals(status)) {
                    try {
                        shortlistedCandidateRepository.deleteById(existingId);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            // Save main Candidate details
            Candidate savedCandidate = candidateRepository.save(candidate);
            
            // Categorize and copy details into either Eligible or Not Eligible Candidate tables
            if (atsScore >= 80) {
                EligibleCandidate eligible = EligibleCandidate.builder()
                        .id(savedCandidate.getId()) // Synchronize ID
                        .fullName(savedCandidate.getFullName())
                        .email(savedCandidate.getEmail())
                        .phoneNumber(savedCandidate.getPhoneNumber())
                        .location(savedCandidate.getLocation())
                        .linkedinProfile(savedCandidate.getLinkedinProfile())
                        .professionalSummary(savedCandidate.getProfessionalSummary())
                        .educationDetails(savedCandidate.getEducationDetails())
                        .experienceDetails(savedCandidate.getExperienceDetails())
                        .skills(savedCandidate.getSkills())
                        .certifications(savedCandidate.getCertifications())
                        .projects(savedCandidate.getProjects())
                        .languages(savedCandidate.getLanguages())
                        .totalYearsExperience(savedCandidate.getTotalYearsExperience())
                        .atsScore(savedCandidate.getAtsScore())
                        .candidateStatus(savedCandidate.getCandidateStatus())
                        .resumeHash(savedCandidate.getResumeHash())
                        .jobDescription(savedCandidate.getJobDescription())
                        .shortlisted(savedCandidate.isShortlisted())
                        .resumeUploadDate(savedCandidate.getResumeUploadDate())
                        .recruitmentStage(savedCandidate.getRecruitmentStage())
                        .designation(savedCandidate.getDesignation())
                        .salaryPackage(savedCandidate.getSalaryPackage())
                        .joiningDate(savedCandidate.getJoiningDate())
                        .companyPolicies(savedCandidate.getCompanyPolicies())
                        .matchingSkills(savedCandidate.getMatchingSkills())
                        .missingSkills(savedCandidate.getMissingSkills())
                        .strengths(savedCandidate.getStrengths())
                        .improvements(savedCandidate.getImprovements())
                        .feedbackReason(savedCandidate.getFeedbackReason())
                        .build();
                eligibleCandidateRepository.save(eligible);

                // If they were shortlisted and remain eligible, save to shortlisted collection
                if (savedCandidate.isShortlisted()) {
                    ShortlistedCandidate shortlisted = ShortlistedCandidate.builder()
                            .id(savedCandidate.getId()) // Synchronize ID
                            .fullName(savedCandidate.getFullName())
                            .email(savedCandidate.getEmail())
                            .phoneNumber(savedCandidate.getPhoneNumber())
                            .location(savedCandidate.getLocation())
                            .linkedinProfile(savedCandidate.getLinkedinProfile())
                            .professionalSummary(savedCandidate.getProfessionalSummary())
                            .educationDetails(savedCandidate.getEducationDetails())
                            .experienceDetails(savedCandidate.getExperienceDetails())
                            .skills(savedCandidate.getSkills())
                            .certifications(savedCandidate.getCertifications())
                            .projects(savedCandidate.getProjects())
                            .languages(savedCandidate.getLanguages())
                            .totalYearsExperience(savedCandidate.getTotalYearsExperience())
                            .atsScore(savedCandidate.getAtsScore())
                            .candidateStatus(savedCandidate.getCandidateStatus())
                            .shortlisted(savedCandidate.isShortlisted())
                            .resumeHash(savedCandidate.getResumeHash())
                            .jobDescription(savedCandidate.getJobDescription())
                            .resumeUploadDate(savedCandidate.getResumeUploadDate())
                            .recruitmentStage(savedCandidate.getRecruitmentStage())
                            .designation(savedCandidate.getDesignation())
                            .salaryPackage(savedCandidate.getSalaryPackage())
                            .joiningDate(savedCandidate.getJoiningDate())
                            .companyPolicies(savedCandidate.getCompanyPolicies())
                            .matchingSkills(savedCandidate.getMatchingSkills())
                            .missingSkills(savedCandidate.getMissingSkills())
                            .strengths(savedCandidate.getStrengths())
                            .improvements(savedCandidate.getImprovements())
                            .feedbackReason(savedCandidate.getFeedbackReason())
                            .build();
                    shortlistedCandidateRepository.save(shortlisted);
                }
            } else {
                NotEligibleCandidate notEligible = NotEligibleCandidate.builder()
                        .id(savedCandidate.getId()) // Synchronize ID
                        .fullName(savedCandidate.getFullName())
                        .email(savedCandidate.getEmail())
                        .phoneNumber(savedCandidate.getPhoneNumber())
                        .location(savedCandidate.getLocation())
                        .linkedinProfile(savedCandidate.getLinkedinProfile())
                        .professionalSummary(savedCandidate.getProfessionalSummary())
                        .educationDetails(savedCandidate.getEducationDetails())
                        .experienceDetails(savedCandidate.getExperienceDetails())
                        .skills(savedCandidate.getSkills())
                        .certifications(savedCandidate.getCertifications())
                        .projects(savedCandidate.getProjects())
                        .languages(savedCandidate.getLanguages())
                        .totalYearsExperience(savedCandidate.getTotalYearsExperience())
                        .atsScore(savedCandidate.getAtsScore())
                        .candidateStatus(savedCandidate.getCandidateStatus())
                        .resumeHash(savedCandidate.getResumeHash())
                        .jobDescription(savedCandidate.getJobDescription())
                        .shortlisted(savedCandidate.isShortlisted())
                        .resumeUploadDate(savedCandidate.getResumeUploadDate())
                        .recruitmentStage(savedCandidate.getRecruitmentStage())
                        .designation(savedCandidate.getDesignation())
                        .salaryPackage(savedCandidate.getSalaryPackage())
                        .joiningDate(savedCandidate.getJoiningDate())
                        .companyPolicies(savedCandidate.getCompanyPolicies())
                        .matchingSkills(savedCandidate.getMatchingSkills())
                        .missingSkills(savedCandidate.getMissingSkills())
                        .strengths(savedCandidate.getStrengths())
                        .improvements(savedCandidate.getImprovements())
                        .feedbackReason(savedCandidate.getFeedbackReason())
                        .build();
                notEligibleCandidateRepository.save(notEligible);
            }
            
            return savedCandidate;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing and saving resume: " + e.getMessage(), e);
        }
    }

    private String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) return "";

        String lowerCaseName = filename.toLowerCase();
        try (InputStream is = file.getInputStream()) {
            if (lowerCaseName.endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(is)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if (lowerCaseName.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(is);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            } else if (lowerCaseName.endsWith(".txt")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + filename);
            }
        }
    }

    private String callGeminiAPI(String text, String jobDescription) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || "YOUR_GEMINI_API_KEY_HERE".equals(geminiApiKey)) {
            // Log a clear warning — the app still works using a basic text-based extraction fallback
            System.err.println("====== [WARNING] Gemini API key is NOT configured. Using basic text extraction fallback. ======");
            System.err.println("====== Set 'app.gemini.api-key' in application.properties with your real Gemini API key. ======");
            return buildFallbackJson(text);
        }

        String prompt = "You are an expert Resume Screening and Information Extraction AI.\n\n" +
                "Your task is to extract structured information from the uploaded resume based on matching with a target job description.\n\n" +
                "Job Description for reference:\n" + 
                (jobDescription != null && !jobDescription.trim().isEmpty() ? jobDescription : "General IT/Software Developer role evaluation standard.") + "\n\n" +
                "Return the response ONLY as a valid machine-readable JSON structure:\n" +
                "{\n" +
                "  \"full_name\": \"\",\n" +
                "  \"email\": \"\",\n" +
                "  \"phone\": \"\",\n" +
                "  \"location\": \"\",\n" +
                "  \"linkedin\": \"\",\n" +
                "  \"professional_summary\": \"\",\n" +
                "  \"education_details\": [{\"degree\": \"\", \"specialization\": \"\", \"institution\": \"\", \"graduation_year\": \"\", \"cgpa\": \"\"}],\n" +
                "  \"experience_details\": [{\"company\": \"\", \"job_title\": \"\", \"start_date\": \"\", \"end_date\": \"\", \"duration\": \"\", \"responsibilities\": []}],\n" +
                "  \"skills\": [],\n" +
                "  \"certifications\": [],\n" +
                "  \"projects\": [],\n" +
                "  \"languages\": [],\n" +
                "  \"total_experience\": \"\"\n" +
                "}\n\n" +
                "Important Rules:\n" +
                "* If details are missing, return null or empty values. Do not invent data.\n" +
                "* Output JSON only. Do not wrap the JSON output in markdown formatting.\n\n" +
                "Here is the resume text:\n\n" + text;

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", prompt);
        
        Map<String, Object> contents = new HashMap<>();
        contents.put("parts", List.of(parts));
        
        requestBody.put("contents", List.of(contents));

        try {
            Map response = webClient.post()
                    .uri("/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> resParts = (List<Map<String, Object>>) content.get("parts");
                    if (!resParts.isEmpty()) {
                        String jsonOutput = (String) resParts.get(0).get("text");
                        // Clean up markdown JSON block if present
                        if (jsonOutput.startsWith("```json")) {
                            jsonOutput = jsonOutput.substring(7);
                            if (jsonOutput.endsWith("```")) {
                                jsonOutput = jsonOutput.substring(0, jsonOutput.length() - 3);
                            }
                        } else if (jsonOutput.startsWith("```")) {
                            jsonOutput = jsonOutput.substring(3);
                            if (jsonOutput.endsWith("```")) {
                                jsonOutput = jsonOutput.substring(0, jsonOutput.length() - 3);
                            }
                        }
                        return jsonOutput.trim();
                    }
                }
            }
            throw new RuntimeException("Failed to extract JSON from Gemini API response");
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with AI service: " + e.getMessage(), e);
        }
    }

    private String calculateSHA256(String text) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating SHA-256 hash", e);
        }
    }

    public int calculateDeterministicAtsScore(
            String resumeText, 
            String jobDescription, 
            String totalExperience, 
            String educationJson, 
            String certificationsJson, 
            String projectsJson, 
            String languagesJson, 
            String fullName, 
            String email, 
            String phoneNumber
    ) {
        String jdToUse = (jobDescription != null && !jobDescription.trim().isEmpty()) 
                ? jobDescription : "General IT/Software Developer role evaluation standard.";
        
        String resumeTextLower = resumeText.toLowerCase();
        String jdLower = jdToUse.toLowerCase();

        // 1. Skills Match (30%)
        String[] techDictionary = {
            "java", "spring boot", "spring", "react", "angular", "javascript", "js", "typescript", "ts",
            "python", "django", "flask", "html", "css", "sql", "mysql", "postgresql", "oracle", "mongodb",
            "aws", "azure", "gcp", "docker", "kubernetes", "k8s", "git", "github", "ci/cd", "jenkins",
            "node.js", "nodejs", "node", "express", "rest api", "rest", "api", "microservices", "testing",
            "junit", "c++", "c#", ".net", "php", "laravel", "ruby", "rails", "swift", "kotlin", "android",
            "ios", "flutter", "devops", "agile", "scrum", "jira", "maven", "gradle", "terraform"
        };
        
        java.util.Set<String> requiredSkills = new java.util.HashSet<>();
        for (String skill : techDictionary) {
            if (jdLower.contains(skill)) {
                requiredSkills.add(skill);
            }
        }
        if (requiredSkills.isEmpty()) {
            // Default skills if none match in Job Description
            requiredSkills.addAll(java.util.Arrays.asList("java", "javascript", "sql", "git", "api", "testing"));
        }

        int matchedSkills = 0;
        for (String skill : requiredSkills) {
            if (resumeTextLower.contains(skill)) {
                matchedSkills++;
            }
        }
        double skillsMatchScore = 30.0 * ((double) matchedSkills / requiredSkills.size());

        // 2. Experience Relevance (25%)
        int parsedYears = 0;
        if (totalExperience != null && !totalExperience.trim().isEmpty()) {
            String expLower = totalExperience.toLowerCase();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\d+");
            java.util.regex.Matcher m = p.matcher(expLower);
            if (m.find()) {
                parsedYears = Integer.parseInt(m.group());
            }
        }
        double experienceScore;
        if (parsedYears >= 5) {
            experienceScore = 25.0;
        } else if (parsedYears >= 3) {
            experienceScore = 20.0;
        } else if (parsedYears >= 1) {
            experienceScore = 15.0;
        } else {
            experienceScore = 10.0;
        }

        // 3. Education Qualification (15%)
        double educationScore = 8.0;
        if (educationJson != null && !educationJson.trim().isEmpty()) {
            String eduLower = educationJson.toLowerCase();
            if (eduLower.contains("master") || eduLower.contains("m.tech") || eduLower.contains("ms") || eduLower.contains("mba") || eduLower.contains("post graduate")) {
                educationScore = 15.0;
            } else if (eduLower.contains("bachelor") || eduLower.contains("b.tech") || eduLower.contains("be") || eduLower.contains("bs") || eduLower.contains("degree") || eduLower.contains("graduate")) {
                educationScore = 12.0;
            }
        }

        // 4. Certifications (10%)
        int certsCount = 0;
        if (certificationsJson != null && !certificationsJson.trim().isEmpty()) {
            try {
                JsonNode certsNode = objectMapper.readTree(certificationsJson);
                if (certsNode.isArray()) {
                    certsCount = certsNode.size();
                }
            } catch (Exception e) {
                if (certificationsJson.length() > 5) {
                    certsCount = 1;
                }
            }
        }
        double certificationsScore = certsCount >= 2 ? 10.0 : (certsCount == 1 ? 7.0 : 3.0);

        // 5. Keywords Matching (10%)
        String[] generalKeywords = {
            "agile", "scrum", "development", "testing", "design", "architecture", "collaboration", "communication", "team"
        };
        java.util.Set<String> requiredGeneralKeywords = new java.util.HashSet<>();
        for (String kw : generalKeywords) {
            if (jdLower.contains(kw)) {
                requiredGeneralKeywords.add(kw);
            }
        }
        if (requiredGeneralKeywords.isEmpty()) {
            requiredGeneralKeywords.addAll(java.util.Arrays.asList("development", "testing", "design", "team", "communication"));
        }
        int matchedGeneral = 0;
        for (String kw : requiredGeneralKeywords) {
            if (resumeTextLower.contains(kw)) {
                matchedGeneral++;
            }
        }
        double keywordsScore = 10.0 * ((double) matchedGeneral / requiredGeneralKeywords.size());

        // 6. Resume Completeness (10%)
        double completenessScore = 0.0;
        if (fullName != null && !fullName.trim().isEmpty()) completenessScore += 2.0;
        if (email != null && !email.trim().isEmpty()) completenessScore += 2.0;
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) completenessScore += 2.0;
        if (projectsJson != null && !projectsJson.trim().isEmpty() && !projectsJson.equals("[]") && !projectsJson.equals("null")) completenessScore += 2.0;
        if (languagesJson != null && !languagesJson.trim().isEmpty() && !languagesJson.equals("[]") && !languagesJson.equals("null")) completenessScore += 2.0;

        int totalScore = (int) Math.round(skillsMatchScore + experienceScore + educationScore + certificationsScore + keywordsScore + completenessScore);
        if (totalScore > 100) totalScore = 100;
        if (totalScore < 0) totalScore = 0;

        return totalScore;
    }

    /**
     * Fallback used when no Gemini API key is configured.
     * Performs basic regex extraction of real values from the resume text.
     * Returns a structured JSON with what it can find — no hardcoded dummy data.
     */
    private String buildFallbackJson(String text) {
        String email = "";
        java.util.regex.Matcher emailMatcher = java.util.regex.Pattern
            .compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}").matcher(text);
        if (emailMatcher.find()) email = emailMatcher.group();

        String phone = "";
        java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern
            .compile("(?:\\+91[\\s-]?)?[6-9]\\d{9}|\\d{10}").matcher(text);
        if (phoneMatcher.find()) phone = phoneMatcher.group().replaceAll("[\\s-]", "");

        String linkedin = "";
        java.util.regex.Matcher linkedinMatcher = java.util.regex.Pattern
            .compile("https?://(?:www\\.)?linkedin\\.com/in/[a-zA-Z0-9\\-_%]+").matcher(text);
        if (linkedinMatcher.find()) linkedin = linkedinMatcher.group();

        // 1. Identify Sections & Split
        String textLower = text.toLowerCase();
        int skillsStart = findHeaderIndex(text, new String[]{"skills", "technical skills", "key skills"});
        int eduStart = findHeaderIndex(text, new String[]{"education", "academic qualification", "academic history"});
        int expStart = findHeaderIndex(text, new String[]{"experience", "work experience", "internship", "employment history", "work history"});
        int projStart = findHeaderIndex(text, new String[]{"projects", "academic projects", "key projects"});
        int certStart = findHeaderIndex(text, new String[]{"certificates", "certifications", "credentials"});

        java.util.List<Section> sections = new java.util.ArrayList<>();
        sections.add(new Section("contact", 0));
        if (skillsStart != -1) sections.add(new Section("skills", skillsStart));
        if (eduStart != -1) sections.add(new Section("education", eduStart));
        if (expStart != -1) sections.add(new Section("experience", expStart));
        if (projStart != -1) sections.add(new Section("projects", projStart));
        if (certStart != -1) sections.add(new Section("certifications", certStart));

        sections.sort((a, b) -> Integer.compare(a.start, b.start));

        for (int i = 0; i < sections.size(); i++) {
            int end = text.length();
            if (i < sections.size() - 1) {
                end = sections.get(i + 1).start;
            }
            sections.get(i).end = end;
        }

        String contactText = getSectionContent(sections, "contact", text);
        String skillsText = getSectionContent(sections, "skills", text);
        String eduText = getSectionContent(sections, "education", text);
        String expText = getSectionContent(sections, "experience", text);
        String projText = getSectionContent(sections, "projects", text);
        String certText = getSectionContent(sections, "certifications", text);

        // 2. Parse Contact Section
        String fullName = "";
        String[] contactLines = contactText.split("\\n");
        for (String line : contactLines) {
            String cleanLine = line.trim();
            String lower = cleanLine.toLowerCase();
            if (!cleanLine.isEmpty() && cleanLine.length() > 2 && cleanLine.length() < 40 
                && !cleanLine.contains("@") && !cleanLine.contains("/") && !cleanLine.contains("\\") 
                && !cleanLine.contains(":")
                && !lower.contains("resume") && !lower.contains("curriculum")
                && !lower.contains("developer") && !lower.contains("engineer")
                && !lower.contains("designer") && !lower.contains("manager")
                && !lower.contains("architect") && !lower.contains("analyst")
                && !lower.contains("intern") && !lower.contains("specialist")
                && !lower.contains("full stack") && !lower.contains("phone")
                && !lower.contains("email") && !lower.contains("mobile")) {
                fullName = cleanLine;
                break;
            }
        }

        String location = "";
        for (String line : contactLines) {
            String cleanLine = line.trim();
            String lower = cleanLine.toLowerCase();
            if (lower.contains("india") || lower.contains("hyderabad") || lower.contains("karimnagar") 
                || lower.contains("bangalore") || lower.contains("bengaluru") || lower.contains("pune") 
                || lower.contains("mumbai") || lower.contains("delhi") || lower.contains("chennai")
                || lower.contains("secunderabad") || lower.contains("telangana") || lower.contains("andhra")) {
                location = cleanLine.replaceAll("^[\\s,·•\\-]+", "").trim();
                break;
            }
        }

        // 3. Experience Years Heuristics
        String totalExperience = "";
        java.util.regex.Pattern expPat = java.util.regex.Pattern.compile("(\\d+\\+?\\s*(?:year|yr)s?\\s*(?:of)?\\s*experience)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher expMat = expPat.matcher(text);
        if (expMat.find()) {
            totalExperience = expMat.group(1).trim();
        }

        // 4. Professional Summary Heuristic
        String professionalSummary = "";
        java.util.regex.Pattern sumPat = java.util.regex.Pattern.compile("(?i)(?:summary|profile|objective)\\s*\\n([^\\n]+(?:\\n[^\\n]+)?)");
        java.util.regex.Matcher sumMat = sumPat.matcher(text);
        if (sumMat.find()) {
            professionalSummary = sumMat.group(1).trim();
        } else {
            if (contactText.length() > 200) {
                professionalSummary = contactText.substring(0, 200).replaceAll("\\s+", " ").trim();
            } else {
                professionalSummary = contactText.replaceAll("\\s+", " ").trim();
            }
        }

        try {
            com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
            root.put("full_name", fullName);
            root.put("email", email);
            root.put("phone", phone);
            root.put("location", location);
            root.put("linkedin", linkedin);
            root.put("professional_summary", professionalSummary);
            root.put("total_experience", totalExperience);

            // Skills
            com.fasterxml.jackson.databind.node.ArrayNode skillsNode = objectMapper.createArrayNode();
            String[] skillWords = {
                "Java", "Spring Boot", "Spring", "React", "Angular", "JavaScript", "TypeScript",
                "Python", "Django", "Flask", "HTML", "CSS", "SQL", "MySQL", "PostgreSQL", "MongoDB",
                "AWS", "Azure", "Docker", "Kubernetes", "Git", "GitHub", "Jenkins", "Node.js", "Express",
                "C++", "C#", ".NET", "PHP", "Laravel", "DevOps", "Agile", "Scrum", "REST API", "Linux",
                "Hibernate", "Redux", "Bootstrap", "MUI", "Material-UI", "JSON"
            };
            String skillsSource = skillsText.isEmpty() ? textLower : skillsText.toLowerCase();
            for (String skill : skillWords) {
                if (skillsSource.contains(skill.toLowerCase())) {
                    skillsNode.add(skill);
                }
            }
            root.set("skills", skillsNode);

            // Education details
            com.fasterxml.jackson.databind.node.ArrayNode eduNode = objectMapper.createArrayNode();
            String sourceEdu = eduText;
            if (!sourceEdu.isEmpty()) {
                String[] eduLines = sourceEdu.split("\\n");
                String degree = "";
                String institution = "";
                String gradYear = "";
                
                for (String line : eduLines) {
                    String cleanLine = line.trim();
                    if (cleanLine.isEmpty()) continue;
                    
                    String lower = cleanLine.toLowerCase();
                    if (lower.contains("bachelor") || lower.contains("b.tech") 
                        || lower.contains("master") || lower.contains("m.tech") 
                        || lower.contains("degree") || lower.contains("diploma")
                        || lower.contains("b.e.") || lower.contains("b.s.")
                        || lower.contains("m.s.") || lower.contains("m.e.")) {
                        if (!degree.isEmpty()) {
                            com.fasterxml.jackson.databind.node.ObjectNode eduObj = objectMapper.createObjectNode();
                            eduObj.put("degree", degree);
                            eduObj.put("specialization", "Technology");
                            eduObj.put("institution", institution);
                            eduObj.put("graduation_year", gradYear);
                            eduObj.put("cgpa", "");
                            eduNode.add(eduObj);
                            institution = "";
                            gradYear = "";
                        }
                        degree = cleanLine;
                    } else if (lower.contains("institute") || lower.contains("college") 
                        || lower.contains("university") || lower.contains("school") 
                        || lower.contains("academy") || lower.contains("science")) {
                        institution = cleanLine;
                    } else if (cleanLine.matches(".*\\b(20\\d{2}|19\\d{2})\\b.*")) {
                        java.util.regex.Matcher yrMat = java.util.regex.Pattern.compile("\\b(20\\d{2}|19\\d{2})\\b").matcher(cleanLine);
                        while (yrMat.find()) {
                            gradYear = yrMat.group(1);
                        }
                    }
                }
                if (!degree.isEmpty()) {
                    com.fasterxml.jackson.databind.node.ObjectNode eduObj = objectMapper.createObjectNode();
                    eduObj.put("degree", degree);
                    eduObj.put("specialization", "Technology");
                    eduObj.put("institution", institution);
                    eduObj.put("graduation_year", gradYear);
                    eduObj.put("cgpa", "");
                    eduNode.add(eduObj);
                }
            }
            root.set("education_details", eduNode);

            // Experience details
            com.fasterxml.jackson.databind.node.ArrayNode expNode = objectMapper.createArrayNode();
            String sourceExp = expText;
            if (!sourceExp.isEmpty()) {
                String[] expLines = sourceExp.split("\\n");
                String title = "";
                String company = "";
                String duration = "";
                java.util.List<String> responsibilities = new java.util.ArrayList<>();
                
                for (String line : expLines) {
                    String cleanLine = line.trim();
                    if (cleanLine.isEmpty()) continue;
                    
                    String lower = cleanLine.toLowerCase();
                    if (lower.equals("experience") || lower.equals("work experience") 
                        || lower.equals("internship") || lower.equals("projects")
                        || lower.equals("discription") || lower.equals("description")) {
                        continue;
                    }
                    
                    boolean isBullet = cleanLine.startsWith("•") || cleanLine.startsWith("-") || cleanLine.startsWith("*") 
                                    || cleanLine.startsWith("▪") || cleanLine.startsWith("◦") || cleanLine.startsWith("\uFFFD")
                                    || (cleanLine.length() > 0 && cleanLine.charAt(0) == '\uFFFD');
                    
                    if (isBullet) {
                        String resp = cleanLine.substring(1).trim();
                        if (!resp.isEmpty()) {
                            responsibilities.add(resp);
                        }
                    } else if (lower.contains("developer") || lower.contains("engineer") 
                        || lower.contains("intern") || lower.contains("analyst") 
                        || lower.contains("manager") || lower.contains("architect")
                        || lower.contains("lead") || lower.contains("programmer")) {
                        
                        if (!title.isEmpty()) {
                            com.fasterxml.jackson.databind.node.ObjectNode expObj = objectMapper.createObjectNode();
                            expObj.put("company", company);
                            expObj.put("job_title", title);
                            expObj.put("start_date", "");
                            expObj.put("end_date", "");
                            expObj.put("duration", duration);
                            com.fasterxml.jackson.databind.node.ArrayNode respNode = objectMapper.createArrayNode();
                            for (String r : responsibilities) respNode.add(r);
                            expObj.set("responsibilities", respNode);
                            expNode.add(expObj);
                            
                            company = "";
                            duration = "";
                            responsibilities.clear();
                        }
                        title = cleanLine;
                    } else {
                        if (company.isEmpty() && (lower.contains("solutions") || lower.contains("technologies") 
                            || lower.contains("pvt") || lower.contains("ltd") || lower.contains("software") 
                            || lower.contains("inc") || lower.contains("nerostech") || lower.contains("corp") 
                            || lower.contains("company") || lower.contains("limited"))) {
                            company = cleanLine;
                        } else if (duration.isEmpty() && (lower.contains("year") || lower.contains("month") 
                            || (lower.contains("20") && lower.contains("present")) || lower.contains(" – ") || lower.contains(" - "))) {
                            duration = cleanLine;
                        } else {
                            if (!responsibilities.isEmpty()) {
                                int lastIdx = responsibilities.size() - 1;
                                responsibilities.set(lastIdx, responsibilities.get(lastIdx) + " " + cleanLine);
                            } else if (!title.isEmpty() && company.isEmpty()) {
                                company = cleanLine;
                            } else {
                                responsibilities.add(cleanLine);
                            }
                        }
                    }
                }
                if (!title.isEmpty()) {
                    com.fasterxml.jackson.databind.node.ObjectNode expObj = objectMapper.createObjectNode();
                    expObj.put("company", company);
                    expObj.put("job_title", title);
                    expObj.put("start_date", "");
                    expObj.put("end_date", "");
                    expObj.put("duration", duration);
                    com.fasterxml.jackson.databind.node.ArrayNode respNode = objectMapper.createArrayNode();
                    for (String r : responsibilities) respNode.add(r);
                    expObj.set("responsibilities", respNode);
                    expNode.add(expObj);
                }
            }
            root.set("experience_details", expNode);

            // Certifications
            com.fasterxml.jackson.databind.node.ArrayNode certNode = objectMapper.createArrayNode();
            String sourceCert = certText;
            if (sourceCert.isEmpty()) {
                String[] allLines = text.split("\\n");
                for (String line : allLines) {
                    String cleanLine = line.trim();
                    if (cleanLine.isEmpty()) continue;
                    String lower = cleanLine.toLowerCase();
                    if (lower.contains("certificate") || lower.contains("certification") || lower.contains("certified")) {
                        certNode.add(cleanLine);
                    }
                }
            } else {
                String[] certLines = sourceCert.split("\\n");
                for (String line : certLines) {
                    String cleanLine = line.trim();
                    if (cleanLine.isEmpty()) continue;
                    String lower = cleanLine.toLowerCase();
                    if (lower.equals("certificates") || lower.equals("certifications") || lower.equals("credentials")) {
                        continue;
                    }
                    if (cleanLine.startsWith("•") || cleanLine.startsWith("-") || cleanLine.startsWith("*") || cleanLine.startsWith("▪") || cleanLine.startsWith("◦") || cleanLine.startsWith("\uFFFD")) {
                        cleanLine = cleanLine.substring(1).trim();
                    }
                    if (!cleanLine.isEmpty()) {
                        certNode.add(cleanLine);
                    }
                }
            }
            root.set("certifications", certNode);

            // Projects
            com.fasterxml.jackson.databind.node.ArrayNode projNode = objectMapper.createArrayNode();
            String sourceProj = projText;
            if (!sourceProj.isEmpty()) {
                String[] projLines = sourceProj.split("\\n");
                String lastLine = "";
                for (String line : projLines) {
                    String cleanLine = line.trim();
                    if (cleanLine.isEmpty()) continue;
                    
                    String lower = cleanLine.toLowerCase();
                    if (lower.contains("experience") || lower.equals("projects") || lower.equals("discription") || lower.equals("description")) {
                        continue;
                    }
                    
                    if (lower.contains("technologies used") || lower.contains("technologies:") || lower.startsWith("tech used")) {
                        if (!lastLine.isEmpty() && !projNode.toString().contains(lastLine)) {
                            projNode.add(lastLine);
                        }
                    } else if (lower.contains("project:") || lower.contains("project -")) {
                        String pName = cleanLine.replaceAll("(?i)project\\s*[:\\-]\\s*", "").trim();
                        if (!pName.isEmpty()) {
                            projNode.add(pName);
                        }
                    } else if (lower.contains("website development") || lower.contains("handinhand") 
                        || lower.contains("y-mart") || lower.contains("e-commerce")) {
                        projNode.add(cleanLine);
                    }
                    
                    lastLine = cleanLine;
                }
            }
            root.set("projects", projNode);

            // Languages
            com.fasterxml.jackson.databind.node.ArrayNode langNode = objectMapper.createArrayNode();
            String[] langs = {"English", "Hindi", "Telugu", "Tamil", "Spanish", "French", "German", "Japanese"};
            for (String lang : langs) {
                if (textLower.contains(lang.toLowerCase())) {
                    langNode.add(lang);
                }
            }
            root.set("languages", langNode);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"full_name\":\"" + fullName + "\",\"email\":\"" + email + "\",\"phone\":\"" + phone +
                   "\",\"location\":\"" + location + "\",\"linkedin\":\"" + linkedin + "\",\"professional_summary\":\"" + professionalSummary + "\"," +
                   "\"education_details\":[],\"experience_details\":[],\"skills\":[]," +
                   "\"certifications\":[],\"projects\":[],\"languages\":[],\"total_experience\":\"" + totalExperience + "\"}";
        }
    }

    private int findHeaderIndex(String text, String[] headers) {
        String textLower = text.toLowerCase();
        for (String header : headers) {
            String regex = "(?mi)(?:^|\\r|\\n)\\s*" + java.util.regex.Pattern.quote(header) + "\\s*[:\\-]?\\s*(?:\\r|\\n|$)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(textLower);
            if (matcher.find()) {
                int start = matcher.start();
                while (start < text.length() && (text.charAt(start) == '\r' || text.charAt(start) == '\n' || Character.isWhitespace(text.charAt(start)))) {
                    start++;
                }
                return start;
            }
        }
        return -1;
    }

    private String getSectionContent(java.util.List<Section> sections, String name, String text) {
        for (Section sec : sections) {
            if (sec.name.equals(name)) {
                String secText = text.substring(sec.start, sec.end).trim();
                int firstNewLine = secText.indexOf("\n");
                if (firstNewLine != -1) {
                    return secText.substring(firstNewLine).trim();
                }
                return secText;
            }
        }
        return "";
    }

    private static class Section {
        String name;
        int start;
        int end;
        Section(String name, int start) {
            this.name = name;
            this.start = start;
        }
    }

    private void populateAtsFeedback(Candidate candidate, String extractedText) {
        String jdToUse = (candidate.getJobDescription() != null && !candidate.getJobDescription().trim().isEmpty()) 
                ? candidate.getJobDescription() : "General IT/Software Developer role evaluation standard.";
        
        String resumeTextLower = extractedText.toLowerCase();
        String jdLower = jdToUse.toLowerCase();

        // 1. Skill analysis
        String[] techDictionary = {
            "java", "spring boot", "spring", "react", "angular", "javascript", "js", "typescript", "ts",
            "python", "django", "flask", "html", "css", "sql", "mysql", "postgresql", "oracle", "mongodb",
            "aws", "azure", "gcp", "docker", "kubernetes", "k8s", "git", "github", "ci/cd", "jenkins",
            "node.js", "nodejs", "node", "express", "rest api", "rest", "api", "microservices", "testing",
            "junit", "c++", "c#", ".net", "php", "laravel", "ruby", "rails", "swift", "kotlin", "android",
            "ios", "flutter", "devops", "agile", "scrum", "jira", "maven", "gradle", "terraform"
        };

        java.util.Set<String> requiredSkills = new java.util.HashSet<>();
        for (String skill : techDictionary) {
            if (jdLower.contains(skill)) {
                requiredSkills.add(skill);
            }
        }
        if (requiredSkills.isEmpty()) {
            requiredSkills.addAll(java.util.Arrays.asList("java", "javascript", "sql", "git", "api", "testing"));
        }

        java.util.List<String> matchedList = new java.util.ArrayList<>();
        java.util.List<String> missingList = new java.util.ArrayList<>();
        for (String skill : requiredSkills) {
            if (resumeTextLower.contains(skill)) {
                String capSkill = skill.substring(0, 1).toUpperCase() + skill.substring(1);
                matchedList.add(capSkill);
            } else {
                String capSkill = skill.substring(0, 1).toUpperCase() + skill.substring(1);
                missingList.add(capSkill);
            }
        }

        // 2. Strengths list
        java.util.List<String> strengths = new java.util.ArrayList<>();
        if (matchedList.size() >= 4) {
            strengths.add("Strong technical alignment: matched " + matchedList.size() + " key role requirements.");
        } else if (matchedList.size() > 0) {
            strengths.add("Found solid foundation in core technologies: " + String.join(", ", matchedList));
        }
        
        if (candidate.getTotalYearsExperience() != null && !candidate.getTotalYearsExperience().isEmpty()) {
            strengths.add("Professional experience of " + candidate.getTotalYearsExperience() + " is listed.");
        }
        if (candidate.getEducationDetails() != null && !candidate.getEducationDetails().equals("[]")) {
            strengths.add("Academic background in engineering/technology fields clearly presented.");
        }
        if (candidate.getProjects() != null && !candidate.getProjects().equals("[]")) {
            strengths.add("Hands-on capability demonstrated via projects like " + candidate.getProjects());
        }
        if (candidate.getCertifications() != null && !candidate.getCertifications().equals("[]")) {
            strengths.add("Professional credentials bolstered by industry certifications.");
        }
        if (strengths.isEmpty()) {
            strengths.add("Clear contact and identity information formatted correctly.");
        }

        // 3. Improvements list
        java.util.List<String> improvements = new java.util.ArrayList<>();
        if (!missingList.isEmpty()) {
            improvements.add("Include missing core keywords such as: " + String.join(", ", missingList));
        }
        if (candidate.getCertifications() == null || candidate.getCertifications().equals("[]") || candidate.getCertifications().isEmpty()) {
            improvements.add("Consider acquiring industry-recognized certifications (e.g. AWS, Oracle) to boost profile authority.");
        }
        if (candidate.getProjects() == null || candidate.getProjects().equals("[]") || candidate.getProjects().isEmpty()) {
            improvements.add("Add practical projects with Git repositories to showcase code delivery.");
        }
        if (candidate.getTotalYearsExperience() == null || candidate.getTotalYearsExperience().isEmpty()) {
            improvements.add("Detail internships or development experience to satisfy role tenure requirements.");
        }
        if (improvements.isEmpty()) {
            improvements.add("Format descriptions using bullet points to enhance readability for ATS crawlers.");
        }

        // 4. Feedback Reason
        String feedbackReason;
        if (candidate.getAtsScore() >= 80) {
            feedbackReason = "Candidate qualified for the next round because their resume matches the technical requirements with a score of " 
                + candidate.getAtsScore() + "%. The profile shows strong alignment with " + String.join(", ", matchedList) + ", a valid academic background, and relevant experience.";
        } else {
            feedbackReason = "Candidate did not qualify because their ATS score of " + candidate.getAtsScore() + "% is below our 80% threshold. The resume has mismatching technical keywords and lacks sufficient experience/certifications.";
        }

        try {
            candidate.setMatchingSkills(objectMapper.writeValueAsString(matchedList));
            candidate.setMissingSkills(objectMapper.writeValueAsString(missingList));
            candidate.setStrengths(objectMapper.writeValueAsString(strengths));
            candidate.setImprovements(objectMapper.writeValueAsString(improvements));
            candidate.setFeedbackReason(feedbackReason);
        } catch (Exception e) {
            candidate.setMatchingSkills("[]");
            candidate.setMissingSkills("[]");
            candidate.setStrengths("[]");
            candidate.setImprovements("[]");
            candidate.setFeedbackReason("Evaluation complete.");
        }
    }
}
