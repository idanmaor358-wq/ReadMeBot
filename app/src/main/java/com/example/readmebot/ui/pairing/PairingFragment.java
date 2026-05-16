package com.example.readmebot.ui.pairing;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.readmebot.R;
import com.example.readmebot.databinding.FragmentPairingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PairingFragment extends Fragment {

    private FragmentPairingBinding binding;
    private FirebaseFirestore db;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPairingBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fetch existing pairing code if any
        fetchExistingCode();

        binding.btnGenerateCode.setOnClickListener(v -> generateNewCode());

        binding.btnJoinPartner.setOnClickListener(v -> {
            String code = binding.etPartnerCode.getText().toString().trim().toUpperCase();
            if (!TextUtils.isEmpty(code)) {
                joinPartnerWithCode(code);
            } else {
                binding.tilPartnerCode.setError("Please enter a code");
            }
        });
    }

    private void fetchExistingCode() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("pairingCode")) {
                        binding.tvMyCode.setText(documentSnapshot.getString("pairingCode"));
                    }
                });
    }

    private void generateNewCode() {
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        while (code.length() < 6) {
            code.append(characters.charAt(rnd.nextInt(characters.length())));
        }

        String finalCode = code.toString();
        binding.pairingProgress.setVisibility(View.VISIBLE);

        db.collection("users").document(currentUserId)
                .update("pairingCode", finalCode)
                .addOnSuccessListener(aVoid -> {
                    binding.pairingProgress.setVisibility(View.GONE);
                    binding.tvMyCode.setText(finalCode);
                })
                .addOnFailureListener(e -> {
                    binding.pairingProgress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error generating code", Toast.LENGTH_SHORT).show();
                });
    }

    private void joinPartnerWithCode(String code) {
        binding.pairingProgress.setVisibility(View.VISIBLE);
        
        // Search for user with this code
        db.collection("users")
                .whereEqualTo("pairingCode", code)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot partnerDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String partnerId = partnerDoc.getId();
                        
                        if (partnerId.equals(currentUserId)) {
                            binding.pairingProgress.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "You can't pair with yourself!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        createSharedCouple(partnerId);
                    } else {
                        binding.pairingProgress.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Invalid code. Please check and try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createSharedCouple(String partnerId) {
        String coupleId = db.collection("couples").document().getId();
        
        Map<String, Object> coupleData = new HashMap<>();
        coupleData.put("coupleId", coupleId);
        coupleData.put("partner1", currentUserId);
        coupleData.put("partner2", partnerId);
        coupleData.put("createdAt", FieldValue.serverTimestamp());
        coupleData.put("startDate", FieldValue.serverTimestamp()); // Default

        db.collection("couples").document(coupleId).set(coupleData)
                .addOnSuccessListener(aVoid -> {
                    // Update both users with the new coupleId
                    updateUserCoupleId(currentUserId, coupleId);
                    updateUserCoupleId(partnerId, coupleId);
                    
                    binding.pairingProgress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Connected successfully!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
                })
                .addOnFailureListener(e -> {
                    binding.pairingProgress.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to create connection", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserCoupleId(String userId, String coupleId) {
        db.collection("users").document(userId).update("coupleId", coupleId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
