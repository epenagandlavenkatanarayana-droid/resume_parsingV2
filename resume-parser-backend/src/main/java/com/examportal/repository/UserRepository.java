package com.examportal.repository;

import com.examportal.entity.User;
import com.examportal.enums.Role;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private CollectionReference getUsersCollection() {
        return FirestoreClient.getFirestore().collection("users");
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        try {
            DocumentSnapshot document = getUsersCollection().document(email.toLowerCase().trim()).get().get();
            if (document.exists()) {
                User user = document.toObject(User.class);
                if (user != null) {
                    user.setId(document.getId());
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding user by email in Firestore: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<User> findByRole(Role role) {
        List<User> list = new ArrayList<>();
        try {
            QuerySnapshot querySnapshot = getUsersCollection().whereEqualTo("role", role.name()).get().get();
            for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    user.setId(doc.getId());
                    list.add(user);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding users by role in Firestore: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        try {
            DocumentSnapshot document = getUsersCollection().document(email.toLowerCase().trim()).get().get();
            return document.exists();
        } catch (Exception e) {
            throw new RuntimeException("Error checking user email existence in Firestore: " + e.getMessage(), e);
        }
    }

    public User save(User user) {
        if (user == null || user.getEmail() == null) return null;
        try {
            String docId = user.getEmail().toLowerCase().trim();
            user.setId(docId);
            getUsersCollection().document(docId).set(user).get();
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Error saving user to Firestore: " + e.getMessage(), e);
        }
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try {
            QuerySnapshot querySnapshot = getUsersCollection().get().get();
            for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    user.setId(doc.getId());
                    list.add(user);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding all users in Firestore: " + e.getMessage(), e);
        }
        return list;
    }

    public void delete(User user) {
        if (user == null || user.getEmail() == null) return;
        try {
            getUsersCollection().document(user.getEmail().toLowerCase().trim()).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting user from Firestore: " + e.getMessage(), e);
        }
    }
}
