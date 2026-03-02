package com.example.releasethekraken;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.model.Event;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }
}