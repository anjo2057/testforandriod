package com.example.testingand;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CreateArticleActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText abstractEditText;
    private AutoCompleteTextView categoriesDropdown;
    private Article articleToEdit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_article_form);

        titleEditText = findViewById(R.id.input_article_title);
        abstractEditText = findViewById(R.id.input_article_abstract);
        categoriesDropdown = findViewById(R.id.categoriesDropdown);

        //Dropdown innehåll
        String[] categories = new String[]{"National", "Economy", "Sports", "Technology"};
        ArrayAdapter<String> ddAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                categories
        );
        categoriesDropdown.setAdapter(ddAdapter);

        // Kolla om vi ska ändra en artikel istället för göra ny
        if (getIntent().hasExtra("article")) {
            articleToEdit = (Article) getIntent().getSerializableExtra("article");
            if (articleToEdit != null) {
                // Fyll i formuläret
                setTitle("Edit Article");
                titleEditText.setText(articleToEdit.getTitleText());
                abstractEditText.setText(articleToEdit.getAbstractText());
                categoriesDropdown.setText(articleToEdit.getCategory(), false); // Set dropdown value
            }
        } else {
            setTitle("Create Article");
        }
    }
}
