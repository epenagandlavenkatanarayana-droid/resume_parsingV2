package com.examportal.controller;

import com.examportal.entity.Candidate;
import com.examportal.dto.CandidateResponse;
import com.examportal.payload.ApiResponse;
import com.examportal.repository.CandidateRepository;
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
            
            // 1. Map Education Details
            try {
                if (c.getEducationDetails() != null && !c.getEducationDetails().isEmpty()) {
                    List<Map<String, Object>> eduList = objectMapper.readValue(
                            c.getEducationDetails(), 
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                    res.setEducation(eduList);
                } else {
                    res.setEducation(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setEducation(new ArrayList<>());
            }

            // 2. Map Experience Details (remap job_title to jobTitle)
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
                        formattedExpList.add(formattedExp);
                    }
                    res.setExperience(formattedExpList);
                } else {
                    res.setExperience(new ArrayList<>());
                }
            } catch (Exception ex) {
                res.setExperience(new ArrayList<>());
            }

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
        
        candidate.setShortlisted(!candidate.isShortlisted());
        candidateRepository.save(candidate);
        
        String status = candidate.isShortlisted() ? "shortlisted" : "removed from shortlist";
        return new ResponseEntity<>(new ApiResponse("Candidate " + status, true), HttpStatus.OK);
    }
}
