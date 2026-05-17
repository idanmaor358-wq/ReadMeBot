package com.example.readmebot.ui.signup;

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
import com.example.readmebot.databinding.FragmentSignupBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupFragment extends Fragment {

    private FragmentSignupBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSignupBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        binding.btnDoSignup.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (validateInput(name, email, password)) {
                registerUser(name, email, password);
            }
        });
    }

    private boolean validateInput(String name, String email, String password) {
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            return false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    private void registerUser(String name, String email, String password) {
        binding.btnDoSignup.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Update display name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        user.updateProfile(profileUpdates);
                        
                        // Proceed to Firestore setup
                        createFirestoreUser(user, name, email);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        binding.btnDoSignup.setEnabled(true);
                        Toast.makeText(getContext(), "Signup Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createFirestoreUser(FirebaseUser user, String name, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", user.getUid());
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("coupleId", null);
        userMap.put("mood", "Neutral");
        userMap.put("pairingCode", null);

        db.collection("users").document(user.getUid())
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Welcome to MindLink!", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        binding.btnDoSignup.setEnabled(true);
                        Toast.makeText(getContext(), "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHome() {
        if (getActivity() != null) {
            try {
                // Use the main activity NavController for a seamless jump
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                navController.navigate(R.id.navigation_home);
            } catch (Exception e) {
                // Fallback for safety
                Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
