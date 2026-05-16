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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PairingFragment extends Fragment {

    private FragmentPairingBinding binding;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration userListener;

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

        // REAL-TIME LISTENER: Updates UI instantly when Firestore changes (pairing success or code generation)
        startUserListener();

        binding.btnGenerateCode.setOnClickListener(v -> generateNewCode());

        binding.btnJoinPartner.setOnClickListener(v -> {
            String code = binding.etPartnerCode.getText().toString().trim().toUpperCase();
            if (!TextUtils.isEmpty(code)) {
                joinPartnerWithCode(code);
            } else {
                binding.tilPartnerCode.setError("Please enter a code");
            }
        });

        binding.btnSignOutPairing.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Navigation.findNavController(v).navigate(R.id.navigation_landing);
        });
    }

    private void startUserListener() {
        if (currentUserId == null) return;
        userListener = db.collection("users").document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    // AUTO-NAVIGATE: If a partner connects to us, coupleId will update in Firestore
                    String coupleId = snapshot.getString("coupleId");
                    if (coupleId != null && isAdded()) {
                        Toast.makeText(getContext(), "Partner Connected!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
                        return;
                    }

                    // Update pairing code UI instantly when it changes in DB
                    String code = snapshot.getString("pairingCode");
                    if (code != null) {
                        binding.tvMyCode.setText(code);
                    } else {
                        binding.tvMyCode.setText("----");
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
        binding.btnGenerateCode.setEnabled(false);

        // Update Firestore - our SnapshotListener will catch this and update the text on screen
        db.collection("users").document(currentUserId)
                .update("pairingCode", finalCode)
                .addOnCompleteListener(task -> {
                    if (isAdded()) binding.btnGenerateCode.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "New code generated!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Error: Check Firestore Rules", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void joinPartnerWithCode(String code) {
        binding.btnJoinPartner.setEnabled(false);
        
        // Find the user who has this pairing code
        db.collection("users")
                .whereEqualTo("pairingCode", code)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot partnerDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String partnerId = partnerDoc.getId();
                        
                        if (partnerId.equals(currentUserId)) {
                            binding.btnJoinPartner.setEnabled(true);
                            Toast.makeText(getContext(), "You can't pair with your own code!", Toast.LENGTH_SHORT).show();
                        } else {
                            createSharedCouple(partnerId);
                        }
                    } else {
                        binding.btnJoinPartner.setEnabled(true);
                        Toast.makeText(getContext(), "Invalid Code! Make sure your partner generated a code.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        binding.btnJoinPartner.setEnabled(true);
                        Toast.makeText(getContext(), "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        // Batch update: Create couple doc and update BOTH users simultaneously
        WriteBatch batch = db.batch();
        batch.set(db.collection("couples").document(coupleId), coupleData);
        batch.update(db.collection("users").document(currentUserId), "coupleId", coupleId, "pairingCode", null);
        batch.update(db.collection("users").document(partnerId), "coupleId", coupleId, "pairingCode", null);

        batch.commit().addOnSuccessListener(aVoid -> {
            // Navigation will be handled by the startUserListener() snapshot trigger
            // but we add a safety check here.
            if (isAdded()) {
                Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                binding.btnJoinPartner.setEnabled(true);
                Toast.makeText(getContext(), "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
        binding = null;
    }
}
