package com.example.testingand;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

public class CreateArticleActivity extends AppCompatActivity {
    private AutoCompleteTextView categoriesDropdown;
    private TextInputLayout categoriesLayout;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_article_form);

        categoriesLayout = findViewById(R.id.categoriesLayout);
        categoriesDropdown = findViewById(R.id.categoriesDropdown);


        //Dropdown innehåll
        String[] categories = new String[]{"National", "Economy", "Sports", "Technology"};
        ArrayAdapter<String> ddAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                categories
        );
        categoriesDropdown.setAdapter(ddAdapter);
        categoriesDropdown.setText("National", false);
    }
}
