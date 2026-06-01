package com.examportal.repository;

import com.examportal.entity.NotEligibleCandidate;
import com.google.cloud.firestore.CollectionReference;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

@Repository
public class NotEligibleCandidateRepository {

    private CollectionReference getCollection() {
        return FirestoreClient.getFirestore().collection("not_eligible_candidates");
    }

    public NotEligibleCandidate save(NotEligibleCandidate candidate) {
        if (candidate == null) return null;
        try {
            String docId = candidate.getId();
            if (docId == null || docId.isEmpty()) {
                docId = getCollection().document().getId();
                candidate.setId(docId);
            }
            if (candidate.getEmail() != null) {
                candidate.setEmail(candidate.getEmail().toLowerCase().trim());
            }
            getCollection().document(docId).set(candidate).get();
            return candidate;
        } catch (Exception e) {
            throw new RuntimeException("Error saving not eligible candidate to Firestore: " + e.getMessage(), e);
        }
    }
}
