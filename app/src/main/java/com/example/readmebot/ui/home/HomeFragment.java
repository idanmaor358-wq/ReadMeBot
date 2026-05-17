package com.example.readmebot.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.readmebot.R;
import com.example.readmebot.databinding.FragmentHomeBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private ListenerRegistration partnerListener;
    private ListenerRegistration coupleListener;

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

        // Sync all data in real-time
        listenToUserData();

        binding.ivUserProfile.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_profile);
        });

        binding.cardMyMood.setOnClickListener(v -> showMoodSelector());
    }

    private void listenToUserData() {
        String uid = mAuth.getUid();
        if (uid == null) return;
        
        userListener = db.collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String coupleId = snapshot.getString("coupleId");
                    String myName = snapshot.getString("name");
                    String myMood = snapshot.getString("mood");

                    if (isAdded() && binding != null) {
                        binding.tvGreeting.setText("Hi, " + (myName != null ? myName : "there"));
                        binding.tvMyMoodEmoji.setText(getEmojiForMood(myMood));

                        if (coupleId == null) {
                            navigateToPairing();
                        } else {
                            listenToPartnerData(coupleId);
                            listenToCoupleData(coupleId);
                        }
                    }
                });
    }

    private void listenToCoupleData(String coupleId) {
        if (coupleListener != null) coupleListener.remove();
        coupleListener = db.collection("couples").document(coupleId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    
                    Timestamp createdAt = snapshot.getTimestamp("createdAt");
                    if (createdAt != null && isAdded() && binding != null) {
                        long diffInMillies = Math.abs(new Date().getTime() - createdAt.toDate().getTime());
                        long days = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                        binding.tvJourneyCounter.setText(days + " Days");
                    }
                });
    }

    private void listenToPartnerData(String coupleId) {
        if (partnerListener != null) partnerListener.remove();
        partnerListener = db.collection("users")
                .whereEqualTo("coupleId", coupleId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        if (!doc.getId().equals(mAuth.getUid())) {
                            String partnerName = doc.getString("name");
                            String partnerMood = doc.getString("mood");
                            
                            if (isAdded() && binding != null) {
                                binding.tvPartnerNickname.setText("Paired with: " + (partnerName != null ? partnerName : "Partner"));
                                binding.tvPartnerLabel.setText(partnerName != null ? partnerName : "Partner");
                                binding.tvPartnerMoodEmoji.setText(getEmojiForMood(partnerMood));
                            }
                        }
                    }
                });
    }

    private void navigateToPairing() {
        if (getView() == null) return;
        NavController navController = Navigation.findNavController(getView());
        if (navController.getCurrentDestination() != null && 
            navController.getCurrentDestination().getId() == R.id.navigation_home) {
            navController.navigate(R.id.navigation_pairing);
        }
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
        String uid = mAuth.getUid();
        if (uid == null) return;
        String[] moods = {"Happy", "Sad", "Angry", "Tired", "Busy"};
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String current = doc.getString("mood");
            int nextIndex = 0;
            for (int i = 0; i < moods.length; i++) {
                if (moods[i].equals(current)) {
                    nextIndex = (i + 1) % moods.length;
                    break;
                }
            }
            db.collection("users").document(uid).update("mood", moods[nextIndex]);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
        if (partnerListener != null) partnerListener.remove();
        if (coupleListener != null) coupleListener.remove();
        binding = null;
    }
}
