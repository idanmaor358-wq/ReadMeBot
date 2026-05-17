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
import androidx.navigation.NavController;
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
    private boolean isNavigating = false;

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

        // REAL-TIME LISTENER: This handles navigation automatically when pairing succeeds (for both partners)
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
            navigateToLanding();
        });
    }

    private void startUserListener() {
        if (currentUserId == null) return;
        userListener = db.collection("users").document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists() || isNavigating) return;

                    // If coupleId is found, it means pairing happened (either we did it or partner did it)
                    String coupleId = snapshot.getString("coupleId");
                    if (coupleId != null && isAdded()) {
                        isNavigating = true;
                        Toast.makeText(getContext(), "Connected Successfully!", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                        return;
                    }

                    // Update UI with existing code
                    String code = snapshot.getString("pairingCode");
                    binding.tvMyCode.setText(code != null ? code : "----");
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

        db.collection("users").document(currentUserId)
                .update("pairingCode", finalCode)
                .addOnCompleteListener(task -> {
                    if (isAdded()) {
                        binding.btnGenerateCode.setEnabled(true);
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "New code ready!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void joinPartnerWithCode(String code) {
        if (isNavigating) return;
        binding.btnJoinPartner.setEnabled(false);
        
        db.collection("users")
                .whereEqualTo("pairingCode", code)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || isNavigating) return;

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
                        Toast.makeText(getContext(), "Invalid Code. Ask your partner for their code.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        binding.btnJoinPartner.setEnabled(true);
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        WriteBatch batch = db.batch();
        batch.set(db.collection("couples").document(coupleId), coupleData);
        batch.update(db.collection("users").document(currentUserId), "coupleId", coupleId, "pairingCode", null);
        batch.update(db.collection("users").document(partnerId), "coupleId", coupleId, "pairingCode", null);

        batch.commit().addOnFailureListener(e -> {
            if (isAdded()) {
                binding.btnJoinPartner.setEnabled(true);
                Toast.makeText(getContext(), "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        // Navigation is handled by startUserListener() snapshot trigger automatically!
    }

    private void navigateToHome() {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_home);
        } catch (Exception e) {
            if (isAdded()) Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
        }
    }

    private void navigateToLanding() {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_landing);
        } catch (Exception e) {
            Navigation.findNavController(requireView()).navigate(R.id.navigation_landing);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
        binding = null;
    }
}
