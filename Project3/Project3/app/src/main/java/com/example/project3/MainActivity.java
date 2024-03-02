package com.example.project3;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.ContentValues;


public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 1;

    private EditText editTextUsername, editTextPassword, editTextPhoneNumber, editTextMessage;
    private Button buttonLogin, buttonSend;
    private TextView textViewMessage;
    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewMessage = findViewById(R.id.textViewMessage);


        // Initialize database
        database = openOrCreateDatabase("UserData", MODE_PRIVATE, null);
        // Create a table to store user credentials
        database.execSQL("CREATE TABLE IF NOT EXISTS Users(Username VARCHAR, Password VARCHAR)");

        // Set click listener for the Login button
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
    }

    private void loginUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor cursor = database.rawQuery("SELECT * FROM Users WHERE Username = ?", new String[]{username});
        if (cursor != null && cursor.getCount() > 0) {
            // User exists, check password
            cursor.moveToFirst();
            int passwordIndex = cursor.getColumnIndex("Password");
            if (passwordIndex != -1) {
                String storedPassword = cursor.getString(passwordIndex);
                if (password.equals(storedPassword)) {
                    // Password matches, login successful
                    textViewMessage.setText("Login successful");

                    // Start the database activity
                    Intent intent = new Intent(MainActivity.this, DatabaseActivity.class);
                    startActivity(intent);
                } else {
                    // Password does not match
                    textViewMessage.setText("Invalid password");
                }
            } else {
                // Password column not found
                textViewMessage.setText("Error: Password column not found");
            }
        } else {
            // User does not exist, create new account
            ContentValues values = new ContentValues();
            values.put("Username", username);
            values.put("Password", password);
            long newRowId = database.insert("Users", null, values);
            if (newRowId != -1) {
                // Account created successfully
                textViewMessage.setText("Account created successfully. Please log in.");
            } else {
                // Failed to create account
                textViewMessage.setText("Failed to create account. Please try again.");
            }
        }

        if (cursor != null) {
            cursor.close();
        }
    }

}
