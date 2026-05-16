package com.example.readmebot;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.readmebot.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = binding.navView;
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_landing, R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        
        // Listener to control BottomNavigationView visibility
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.navigation_landing || 
                id == R.id.navigation_login || 
                id == R.id.navigation_signup ||
                id == R.id.navigation_pairing ||
                id == R.id.navigation_profile) {
                navView.setVisibility(View.GONE);
            } else {
                navView.setVisibility(View.VISIBLE);
            }
        });

        NavigationUI.setupWithNavController(navView, navController);
    }
}