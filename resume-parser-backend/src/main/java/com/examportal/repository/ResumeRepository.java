package com.examportal.repository;

import com.examportal.entity.Resume;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ResumeRepository {

    private CollectionReference getCollection() {
        return FirestoreClient.getFirestore().collection("resumes");
    }

    public Optional<Resume> findByUserId(String userId) {
        if (userId == null) return Optional.empty();
        try {
            QuerySnapshot querySnapshot = getCollection().whereEqualTo("userId", userId).get().get();
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                Resume resume = doc.toObject(Resume.class);
                if (resume != null) {
                    resume.setId(doc.getId());
                    return Optional.of(resume);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding resume by userId in Firestore: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Resume save(Resume resume) {
        if (resume == null) return null;
        try {
            String docId = resume.getId();
            if (docId == null || docId.isEmpty()) {
                docId = getCollection().document().getId();
                resume.setId(docId);
            }
            getCollection().document(docId).set(resume).get();
            return resume;
        } catch (Exception e) {
            throw new RuntimeException("Error saving resume to Firestore: " + e.getMessage(), e);
        }
    }
}
