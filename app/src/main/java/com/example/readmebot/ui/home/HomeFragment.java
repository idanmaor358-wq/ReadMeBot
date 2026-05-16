package com.example.readmebot.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.readmebot.R;
import com.example.readmebot.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mAuth.getCurrentUser() == null) return;

        // Listen for user data changes
        listenToUserData();

        // Profile Click Logic - Navigate to Profile Page
        binding.ivUserProfile.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_profile);
        });

        // Set Mood Click Logic
        binding.cardMyMood.setOnClickListener(v -> showMoodSelector());
    }

    private void listenToUserData() {
        String uid = mAuth.getUid();
        userListener = db.collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String coupleId = snapshot.getString("coupleId");
                    String myName = snapshot.getString("name");
                    String myMood = snapshot.getString("mood");

                    // UI Updates
                    if (isAdded()) {
                        binding.tvGreeting.setText("Hi, " + (myName != null ? myName : "there"));
                        binding.tvMyMoodEmoji.setText(getEmojiForMood(myMood));

                        // If not paired, redirect to Pairing screen
                        if (coupleId == null) {
                            Navigation.findNavController(requireView()).navigate(R.id.navigation_pairing);
                        } else {
                            listenToPartnerData(coupleId);
                        }
                    }
                });
    }

    private void listenToPartnerData(String coupleId) {
        db.collection("users")
                .whereEqualTo("coupleId", coupleId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        if (!doc.getId().equals(mAuth.getUid())) {
                            // This is the partner
                            String partnerMood = doc.getString("mood");
                            if (isAdded()) {
                                binding.tvPartnerMoodEmoji.setText(getEmojiForMood(partnerMood));
                            }
                        }
                    }
                });
    }

    private String getEmojiForMood(String mood) {
        if (mood == null) return "😊";
        switch (mood) {
            case "Happy": return "😊";
            case "Sad": return "😢";
            case "Angry": return "😠";
            case "Tired": return "😴";
            case "Busy": return "⏳";
            default: return "😊";
        }
    }

    private void showMoodSelector() {
        String[] moods = {"Happy", "Sad", "Angry", "Tired", "Busy"};
        db.collection("users").document(mAuth.getUid()).get().addOnSuccessListener(doc -> {
            String current = doc.getString("mood");
            int nextIndex = 0;
            for (int i = 0; i < moods.length; i++) {
                if (moods[i].equals(current)) {
                    nextIndex = (i + 1) % moods.length;
                    break;
                }
            }
            db.collection("users").document(mAuth.getUid()).update("mood", moods[nextIndex]);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
        binding = null;
    }
}
