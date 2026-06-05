package com.examportal.repository;

import com.examportal.entity.Candidate;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CandidateRepository {

    private CollectionReference getCandidatesCollection() {
        return FirestoreClient.getFirestore().collection("candidates");
    }

    public Optional<Candidate> findById(String id) {
        if (id == null) return Optional.empty();
        try {
            DocumentSnapshot document = getCandidatesCollection().document(id).get().get();
            if (document.exists()) {
                Candidate candidate = document.toObject(Candidate.class);
                if (candidate != null) {
                    candidate.setId(document.getId());
                    return Optional.of(candidate);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding candidate by ID in Firestore: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Candidate> findByResumeHash(String resumeHash) {
        List<Candidate> list = new ArrayList<>();
        if (resumeHash == null) return list;
        try {
            QuerySnapshot querySnapshot = getCandidatesCollection().whereEqualTo("resumeHash", resumeHash).get().get();
            for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                Candidate candidate = doc.toObject(Candidate.class);
                if (candidate != null) {
                    candidate.setId(doc.getId());
                    list.add(candidate);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding candidate by resume hash in Firestore: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        try {
            QuerySnapshot querySnapshot = getCandidatesCollection().whereEqualTo("email", email.toLowerCase().trim()).get().get();
            return !querySnapshot.isEmpty();
        } catch (Exception e) {
            throw new RuntimeException("Error checking candidate email existence in Firestore: " + e.getMessage(), e);
        }
    }

    public Optional<Candidate> findByEmail(String email) {
        if (email == null) return Optional.empty();
        try {
            QuerySnapshot querySnapshot = getCandidatesCollection().whereEqualTo("email", email.toLowerCase().trim()).get().get();
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                Candidate candidate = doc.toObject(Candidate.class);
                if (candidate != null) {
                    candidate.setId(doc.getId());
                    return Optional.of(candidate);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding candidate by email in Firestore: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Candidate save(Candidate candidate) {
        if (candidate == null) return null;
        try {
            String docId = candidate.getId();
            if (docId == null || docId.isEmpty()) {
                docId = getCandidatesCollection().document().getId();
                candidate.setId(docId);
            }
            if (candidate.getEmail() != null) {
                candidate.setEmail(candidate.getEmail().toLowerCase().trim());
            }
            getCandidatesCollection().document(docId).set(candidate).get();
            return candidate;
        } catch (Exception e) {
            throw new RuntimeException("Error saving candidate to Firestore: " + e.getMessage(), e);
        }
    }

    public List<Candidate> findAll() {
        List<Candidate> list = new ArrayList<>();
        try {
            QuerySnapshot querySnapshot = getCandidatesCollection().get().get();
            for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                Candidate candidate = doc.toObject(Candidate.class);
                if (candidate != null) {
                    candidate.setId(doc.getId());
                    list.add(candidate);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding all candidates in Firestore: " + e.getMessage(), e);
        }
        return list;
    }

    public void delete(Candidate candidate) {
        if (candidate == null || candidate.getId() == null) return;
        try {
            getCandidatesCollection().document(candidate.getId()).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting candidate from Firestore: " + e.getMessage(), e);
        }
    }
}
