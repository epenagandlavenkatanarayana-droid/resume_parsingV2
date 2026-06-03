package com.examportal.repository;

import com.examportal.entity.EligibleCandidate;
import com.google.cloud.firestore.CollectionReference;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

@Repository
public class EligibleCandidateRepository {

    private CollectionReference getCollection() {
        return FirestoreClient.getFirestore().collection("eligible_candidates");
    }

    public EligibleCandidate save(EligibleCandidate candidate) {
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
            throw new RuntimeException("Error saving eligible candidate to Firestore: " + e.getMessage(), e);
        }
    }

    public void deleteById(String id) {
        if (id == null || id.isEmpty()) return;
        try {
            getCollection().document(id).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting eligible candidate from Firestore: " + e.getMessage(), e);
        }
    }
}
