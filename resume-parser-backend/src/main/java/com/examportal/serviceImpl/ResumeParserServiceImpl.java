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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.examportal.entity.Candidate;
import com.examportal.entity.EligibleCandidate;
import com.examportal.entity.NotEligibleCandidate;
import com.examportal.repository.CandidateRepository;
import com.examportal.repository.EligibleCandidateRepository;
import com.examportal.repository.NotEligibleCandidateRepository;

@Service
public class ResumeParserServiceImpl implements ResumeParserService {

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final CandidateRepository candidateRepository;
    private final EligibleCandidateRepository eligibleCandidateRepository;
    private final NotEligibleCandidateRepository notEligibleCandidateRepository;
    private final ObjectMapper objectMapper;

    public ResumeParserServiceImpl(WebClient.Builder webClientBuilder, 
                                   CandidateRepository candidateRepository, 
                                   EligibleCandidateRepository eligibleCandidateRepository, 
                                   NotEligibleCandidateRepository notEligibleCandidateRepository, 
                                   ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.candidateRepository = candidateRepository;
        this.eligibleCandidateRepository = eligibleCandidateRepository;
        this.notEligibleCandidateRepository = notEligibleCandidateRepository;
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
            
            Candidate candidate = new Candidate();
            
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

            if (email != null && !email.isEmpty() && candidateRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email is already registered.");
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
            
            // Save main Candidate details
            Candidate savedCandidate = candidateRepository.save(candidate);
            
            // Categorize and copy details into either Eligible or Not Eligible Candidate tables
            if (atsScore >= 80) {
                EligibleCandidate eligible = EligibleCandidate.builder()
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
                        .build();
                eligibleCandidateRepository.save(eligible);
            } else {
                NotEligibleCandidate notEligible = NotEligibleCandidate.builder()
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

    private int calculateDeterministicAtsScore(
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
        // Extract email
        String email = "";
        java.util.regex.Matcher emailMatcher = java.util.regex.Pattern
            .compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}").matcher(text);
        if (emailMatcher.find()) email = emailMatcher.group();

        // Extract phone (10-digit Indian mobile or international)
        String phone = "";
        java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern
            .compile("(?:\\+91[\\s-]?)?[6-9]\\d{9}|\\d{10}").matcher(text);
        if (phoneMatcher.find()) phone = phoneMatcher.group().replaceAll("[\\s-]", "");

        // Extract LinkedIn URL
        String linkedin = "";
        java.util.regex.Matcher linkedinMatcher = java.util.regex.Pattern
            .compile("https?://(?:www\\.)?linkedin\\.com/in/[a-zA-Z0-9\\-_%]+").matcher(text);
        if (linkedinMatcher.find()) linkedin = linkedinMatcher.group();

        try {
            com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
            root.put("full_name", "");
            root.put("email", email);
            root.put("phone", phone);
            root.put("location", "");
            root.put("linkedin", linkedin);
            root.put("professional_summary", "");
            root.set("education_details", objectMapper.createArrayNode());
            root.set("experience_details", objectMapper.createArrayNode());
            root.set("skills", objectMapper.createArrayNode());
            root.set("certifications", objectMapper.createArrayNode());
            root.set("projects", objectMapper.createArrayNode());
            root.set("languages", objectMapper.createArrayNode());
            root.put("total_experience", "");
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"full_name\":\"\",\"email\":\"" + email + "\",\"phone\":\"" + phone +
                   "\",\"location\":\"\",\"linkedin\":\"\",\"professional_summary\":\"\"," +
                   "\"education_details\":[],\"experience_details\":[],\"skills\":[]," +
                   "\"certifications\":[],\"projects\":[],\"languages\":[],\"total_experience\":\"\"}";
        }
    }
}
