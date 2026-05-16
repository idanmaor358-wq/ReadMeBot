package com.example.readmebot.ui.login;

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
import com.example.readmebot.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        binding.btnDoLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                binding.tilEmail.setError("Email is required");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                binding.tilPassword.setError("Password is required");
                return;
            }

            performLogin(email, password);
        });
    }

    private void performLogin(String email, String password) {
        // Removed progress bar as per user request to fix hanging issues
        binding.btnDoLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (isAdded()) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Welcome back!", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
                        } else {
                            binding.btnDoLogin.setEnabled(true);
                            Toast.makeText(getContext(), "Login failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
