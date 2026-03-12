package com.example.releasethekraken.view;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentCreateEventBinding;
import com.example.releasethekraken.databinding.FragmentViewProfileBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.repository.ProfileRepository;

public class CreateEventFragment extends Fragment {

    private FragmentCreateEventBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().show();
            }
        }

        // Navigate back to main menu
        binding.cancelEventCreation.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_createEventFragment_to_browseEventsFragment)
        );
    }


}