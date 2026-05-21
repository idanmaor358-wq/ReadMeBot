package com.example.readmebot.ui.mood;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.readmebot.R;
import com.example.readmebot.databinding.FragmentMoodSelectorBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MoodSelectorFragment extends Fragment {

    private FragmentMoodSelectorBinding binding;
    private FirebaseFirestore db;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMoodSelectorBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackMood.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        binding.cardHappy.setOnClickListener(v -> updateMood("Happy"));
        binding.cardSad.setOnClickListener(v -> updateMood("Sad"));
        binding.cardAngry.setOnClickListener(v -> updateMood("Angry"));
        binding.cardTired.setOnClickListener(v -> updateMood("Tired"));
        binding.cardBusy.setOnClickListener(v -> updateMood("Busy"));
    }

    private void updateMood(String mood) {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId)
                .update("mood", mood)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Mood updated to " + mood, Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to update mood", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
