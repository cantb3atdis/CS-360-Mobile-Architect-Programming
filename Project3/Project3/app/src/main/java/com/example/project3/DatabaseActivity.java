package com.example.project3;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.telephony.SmsManager;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.ContextCompat;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class DatabaseActivity extends AppCompatActivity {

    private SQLiteDatabase database;
    private ListView listViewWeights;
    private ArrayAdapter<String> adapter;
    private List<String> weightList;
    private double goalWeight = 0.0; // Default goal weight
    private static final int REQUEST_SEND_SMS_PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        // Initialize UI components
        listViewWeights = findViewById(R.id.listViewWeights);
        Button buttonAddEntry = findViewById(R.id.buttonAddEntry);
        Button buttonUpdate = findViewById(R.id.buttonUpdate);
        Button buttonDelete = findViewById(R.id.buttonDelete);
        Button buttonRead = findViewById(R.id.buttonRead);
        Button buttonSetGoal = findViewById(R.id.buttonSetGoal); // New button for setting the goal

        // Set click listener for the "Set Goal" button
        buttonSetGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetGoalDialog();
            }
        });

        buttonAddEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEntryDialog();
            }
        });

        // Set click listeners for CRUD buttons
        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle update button click
                // For simplicity, let's assume the first entry in the list is the one to be updated
                if (!weightList.isEmpty()) {
                    String entry = weightList.get(0);
                    String[] parts = entry.split(" - ");
                    String date = parts[0];
                    double weight = Double.parseDouble(parts[1].split(" ")[0]); // Extract weight value
                    double newWeight = weight + 1.0; // Increment weight by 1
                    updateWeightEntry(date, newWeight);
                } else {
                    Toast.makeText(DatabaseActivity.this, "No entries to update", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle delete button click
                // For simplicity, let's assume the first entry in the list is the one to be deleted
                if (!weightList.isEmpty()) {
                    String entry = weightList.get(0);
                    String[] parts = entry.split(" - ");
                    String date = parts[0];
                    deleteWeightEntry(date);
                } else {
                    Toast.makeText(DatabaseActivity.this, "No entries to delete", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle read button click
                // For simplicity, let's assume the first entry in the list is the one to be read
                if (!weightList.isEmpty()) {
                    String entry = weightList.get(0);
                    String[] parts = entry.split(" - ");
                    String date = parts[0];
                    readWeightEntry(date);
                } else {
                    Toast.makeText(DatabaseActivity.this, "No entries to read", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Initialize database
        database = openOrCreateDatabase("WeightTrackerDB", MODE_PRIVATE, null);

        // Create weight table if not exists
        database.execSQL("CREATE TABLE IF NOT EXISTS WeightEntries(Date TEXT, Weight REAL)");

        // Initialize weight list
        weightList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, weightList);
        listViewWeights.setAdapter(adapter);

        // Load weight entries from database
        loadWeightEntries();
    }



    // Method to show a dialog for setting the goal weight
    private void showSetGoalDialog() {
        // Create a custom layout for the dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_goal, null);

        // Find EditText in the dialog layout
        final EditText editTextGoalWeight = dialogView.findViewById(R.id.editTextGoalWeight);

        // Build the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("Set Goal Weight")
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get the entered goal weight
                        String goalWeightString = editTextGoalWeight.getText().toString().trim();

                        // Validate input
                        if (!goalWeightString.isEmpty()) {
                            // Parse goal weight to double
                            goalWeight = Double.parseDouble(goalWeightString);
                            Toast.makeText(DatabaseActivity.this, "Goal weight set successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(DatabaseActivity.this, "Please enter a valid goal weight", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        // Show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Method to load weight entries from the database
    private void loadWeightEntries() {
        // Clear previous entries
        weightList.clear();

        // Query database for weight entries
        Cursor cursor = database.rawQuery("SELECT * FROM WeightEntries", null);
        if (cursor.moveToFirst()) {
            int dateIndex = cursor.getColumnIndex("Date");
            int weightIndex = cursor.getColumnIndex("Weight");

            // Check if the column exists in the cursor
            if (dateIndex != -1 && weightIndex != -1) {
                do {
                    // Extract data from cursor
                    String date = cursor.getString(dateIndex);
                    double weight = cursor.getDouble(weightIndex);

                    // Add entry to list
                    weightList.add(date + " - " + weight + " kg");
                } while (cursor.moveToNext());
            } else {
                // Handle case where column doesn't exist
                Toast.makeText(this, "Column not found in cursor", Toast.LENGTH_SHORT).show();
            }
        }
        cursor.close();

        // Notify adapter about data change
        adapter.notifyDataSetChanged();
    }

    // Method to show a dialog for adding a new entry
    private void showAddEntryDialog() {
        // Create a custom layout for the dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_entry, null);

        // Find EditTexts in the dialog layout
        final EditText editTextDate = dialogView.findViewById(R.id.editTextDate);
        final EditText editTextWeight = dialogView.findViewById(R.id.editTextWeight);

        // Build the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("Add New Entry")
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get the entered data from EditTexts
                        String date = editTextDate.getText().toString().trim();
                        String weightString = editTextWeight.getText().toString().trim();

                        // Validate input
                        if (date.isEmpty() || weightString.isEmpty()) {
                            Toast.makeText(DatabaseActivity.this, "Please enter both date and weight", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Parse weight to double
                        double weight = Double.parseDouble(weightString);

                        // Add the entry to the database
                        addWeightEntry(date, weight);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        // Show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Method to add a new weight entry to the database
    private void addWeightEntry(String date, double weight) {
        ContentValues values = new ContentValues();
        values.put("Date", date);
        values.put("Weight", weight);

        long newRowId = database.insert("WeightEntries", null, values);
        if (newRowId != -1) {
            Toast.makeText(this, "Entry added successfully", Toast.LENGTH_SHORT).show();
            loadWeightEntries(); // Reload weight entries

            // Check if goal weight is achieved
            if (weight >= goalWeight) {
                triggerNotification(); // Trigger notification if goal weight is achieved
            }
        } else {
            Toast.makeText(this, "Failed to add entry", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to update a weight entry in the database
    private void updateWeightEntry(String date, double newWeight) {
        // Update the weight entry in the database
        ContentValues values = new ContentValues();
        values.put("Weight", newWeight);
        int rowsAffected = database.update("WeightEntries", values, "Date=?", new String[]{date});

        if (rowsAffected > 0) {
            Toast.makeText(this, "Entry updated successfully", Toast.LENGTH_SHORT).show();
            loadWeightEntries(); // Reload weight entries
        } else {
            Toast.makeText(this, "Failed to update entry", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to delete a weight entry from the database
    private void deleteWeightEntry(String date) {
        // Delete the weight entry from the database
        int rowsAffected = database.delete("WeightEntries", "Date=?", new String[]{date});

        if (rowsAffected > 0) {
            Toast.makeText(this, "Entry deleted successfully", Toast.LENGTH_SHORT).show();
            loadWeightEntries(); // Reload weight entries
        } else {
            Toast.makeText(this, "Failed to delete entry", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to read a specific weight entry from the database
    private void readWeightEntry(String date) {
        // Query database for a specific weight entry
        Cursor cursor = database.rawQuery("SELECT * FROM WeightEntries WHERE Date=?", new String[]{date});
        if (cursor.moveToFirst()) {
            int dateIndex = cursor.getColumnIndex("Date");
            int weightIndex = cursor.getColumnIndex("Weight");

            if (dateIndex != -1 && weightIndex != -1) {
                do {
                    // Extract data from cursor
                    String dateValue = cursor.getString(dateIndex);
                    double weight = cursor.getDouble(weightIndex);

                    // Display the weight entry
                    Toast.makeText(this, "Date: " + dateValue + ", Weight: " + weight + " kg", Toast.LENGTH_SHORT).show();
                } while (cursor.moveToNext());
            } else {
                // Handle case where column indices are not found
                Toast.makeText(this, "Date or Weight column not found", Toast.LENGTH_SHORT).show();
            }
        }
        cursor.close();
    }

    // Method to trigger a notification when the goal weight is achieved
    private void triggerNotification() {
        // Check if the app has permission to send SMS
        if (checkPermission(Manifest.permission.SEND_SMS)) {
            // Permission granted, show the notification as an SMS
            sendSMSNotification();
        } else {
            // Permission not granted, request permission from the user
            requestPermission();
        }
    }

    // Method to check if the app has a specific permission
    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    // Method to request permission from the user
    private void requestPermission() {
        // Request permission to send SMS
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS},
                REQUEST_SEND_SMS_PERMISSION);
    }
    private void sendSMS() {
        SmsManager smsManager = SmsManager.getDefault();
        String phoneNumber = "1234567890"; // Replace with the recipient's phone number
        String message = "Congratulations! You have reached your goal weight.";
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }
    // Method to handle the result of permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SEND_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with sending SMS
                sendSMS();
            } else {
                // Permission denied, handle accordingly (show message on screen)
                showNotificationWithoutSMS();
            }
        }
    }

    // Method to send an SMS message
    private void sendSMSNotification() {
        String message = "Congratulations! You have reached your goal weight.";
        String phoneNumber = "your_phone_number_here";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("sms_body", message);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Handle the case where no SMS app is available
            showNotificationWithoutSMS();
        }
    }

    // Method to show the notification without sending an SMS
    private void showNotificationWithoutSMS() {
        // Inflate the notification layout
        View notificationView = LayoutInflater.from(this).inflate(R.layout.activity_notification, null);

        // Build the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(notificationView)
                .setTitle("Goal Achieved!")
                .setMessage("Congratulations! You have reached your goal weight.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        // Show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}
