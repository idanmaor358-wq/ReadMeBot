package com.example.readmebot.ui.landing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.readmebot.R;
import com.example.readmebot.databinding.FragmentLandingBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LandingFragment extends Fragment {

    private FragmentLandingBinding binding;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLandingBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is already signed in
        if (mAuth.getCurrentUser() != null) {
            Navigation.findNavController(requireView()).navigate(R.id.navigation_home);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnLogin.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_landing_to_login);
        });

        binding.btnSignup.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_landing_to_signup);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
