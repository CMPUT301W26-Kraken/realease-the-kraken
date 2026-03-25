package com.example.releasethekraken.view.ui.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentLoginBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.repository.ProfileRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth firebaseAuth;

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

        firebaseAuth = FirebaseAuth.getInstance();

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final Button registerButton = binding.createAccountButton;
        final ProgressBar loadingProgressBar = binding.loading;

        loginButton.setEnabled(true);
        registerButton.setEnabled(true);

        passwordEditText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin(view, usernameEditText, passwordEditText, loadingProgressBar);
                return true;
            }
            return false;
        });

        loginButton.setOnClickListener(v ->
                attemptLogin(view, usernameEditText, passwordEditText, loadingProgressBar)
        );

        registerButton.setOnClickListener(v ->
                attemptRegister(view, usernameEditText, passwordEditText, loadingProgressBar)
        );
    }

    private void attemptLogin(View view,
                              EditText usernameEditText,
                              EditText passwordEditText,
                              ProgressBar loadingProgressBar) {

        String email = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (!isCredentialInputValid(email, password, usernameEditText, passwordEditText)) {
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);
        Log.d("LoginFragment", "Attempting login for: " + email);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    loadingProgressBar.setVisibility(View.GONE);

                    if (!isAdded()) {
                        return;
                    }

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(requireContext(),
                                    "Login failed: user session not created.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        ProfileRepository profileRepository = new ProfileRepository(requireContext());

                        profileRepository.getProfileFromFirestore(new ProfileRepository.ProfileRepositoryCallback<Profile>() {
                            @Override
                            public void onSuccess(Profile profile) {
                                if (!isAdded()) {
                                    return;
                                }

                                Toast.makeText(requireContext(),
                                        "Login successful",
                                        Toast.LENGTH_SHORT).show();

                                Navigation.findNavController(view)
                                        .navigate(R.id.action_loginFragment_to_mainMenuFragment);
                            }

                            @Override
                            public void onFailure(Exception exception) {
                                if (!isAdded()) {
                                    return;
                                }

                                Toast.makeText(requireContext(),
                                        "Logged in successfully. Please create your profile.",
                                        Toast.LENGTH_SHORT).show();

                                Navigation.findNavController(view)
                                        .navigate(R.id.action_loginFragment_to_accountCreateFragment);
                            }
                        });

                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Invalid email or password.";

                        Log.e("LoginFragment", "Login failed", task.getException());
                        Toast.makeText(requireContext(),
                                "Login failed: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void attemptRegister(View view,
                                 EditText usernameEditText,
                                 EditText passwordEditText,
                                 ProgressBar loadingProgressBar) {

        String email = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (!isCredentialInputValid(email, password, usernameEditText, passwordEditText)) {
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);
        Log.d("LoginFragment", "Attempting register for: " + email);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    loadingProgressBar.setVisibility(View.GONE);

                    if (!isAdded()) {
                        return;
                    }

                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(),
                                "Account created successfully. Complete your profile.",
                                Toast.LENGTH_SHORT).show();

                        Navigation.findNavController(view)
                                .navigate(R.id.action_loginFragment_to_accountCreateFragment);
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed.";

                        Log.e("LoginFragment", "Registration failed", task.getException());
                        Toast.makeText(requireContext(),
                                "Registration failed: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isCredentialInputValid(String email,
                                           String password,
                                           EditText usernameEditText,
                                           EditText passwordEditText) {

        boolean isValid = true;

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usernameEditText.setError(getString(R.string.invalid_username));
            isValid = false;
        }

        if (TextUtils.isEmpty(password) || password.length() <= 5) {
            passwordEditText.setError(getString(R.string.invalid_password));
            isValid = false;
        }

        return isValid;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}