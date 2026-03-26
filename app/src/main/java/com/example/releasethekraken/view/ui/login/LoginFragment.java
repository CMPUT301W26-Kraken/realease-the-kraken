package com.example.releasethekraken.view.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentLoginBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.repository.ProfileRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Simplified Login Fragment. 
 * Provides login for existing users and navigation to registration for new users.
 */
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        // Auto-login check
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous()) {
            checkProfileAndNavigate(currentUser.getUid());
        }

        // Login existing user
        binding.login.setOnClickListener(v -> attemptLogin());

        // Navigate to Registration Form
        binding.createAccountButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_accountCreateFragment);
        });
    }

    private void attemptLogin() {
        String email = binding.username.getText().toString().trim();
        String password = binding.password.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.username.setError("Enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.password.setError("Password is required");
            return;
        }

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) checkProfileAndNavigate(user.getUid());
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkProfileAndNavigate(String uid) {
        showLoading(true);
        ProfileRepository repo = new ProfileRepository(requireContext());
        repo.getProfileFromFirestore(uid, new ProfileRepository.ProfileRepositoryCallback<Profile>() {
            @Override
            public void onSuccess(Profile profile) {
                if (!isAdded()) return;
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_loginFragment_to_mainMenuFragment);
            }

            @Override
            public void onFailure(Exception exception) {
                if (!isAdded()) return;
                // User exists in Auth but no profile in Firestore - send to creation
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_loginFragment_to_accountCreateFragment);
            }
        });
    }

    private void showLoading(boolean loading) {
        if (binding == null) return;
        binding.loading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.login.setEnabled(!loading);
        binding.createAccountButton.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}