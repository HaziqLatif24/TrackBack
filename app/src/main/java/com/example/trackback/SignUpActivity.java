package com.example.trackback;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    // Added etRegisterConfirmPassword to the declarations
    private EditText etRegisterName, etRegisterEmail, etRegisterPassword, etRegisterConfirmPassword;
    private Button btnSignUp;
    private TextView tvGoToLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // View Bindings
        etRegisterName = findViewById(R.id.etRegisterName);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword); // Bound confirmation hook
        btnSignUp = findViewById(R.id.btnSignUp);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Click listener routing user safely back to Login screen
        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close SignUpActivity completely
            }
        });

        // Sign Up Execution Block
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String nameStr = etRegisterName.getText().toString().trim();
                String emailStr = etRegisterEmail.getText().toString().trim();
                String passStr = etRegisterPassword.getText().toString().trim();
                String confirmPassStr = etRegisterConfirmPassword.getText().toString().trim();

                // 1. Check if any fields are empty
                if (nameStr.isEmpty() || emailStr.isEmpty() || passStr.isEmpty() || confirmPassStr.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Please fulfill all input fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. Validation: Check if the passwords match match
                if (!passStr.equals(confirmPassStr)) {
                    Toast.makeText(SignUpActivity.this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(emailStr, passStr)
                        .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                                    String uid = mAuth.getCurrentUser().getUid();

                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("fullName", nameStr);
                                    userMap.put("email", mAuth.getCurrentUser().getEmail());

                                    mDatabase.child("users").child(uid).setValue(userMap)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> dbTask) {
                                                    Toast.makeText(SignUpActivity.this, "Account provisioned successfully! Please log in.", Toast.LENGTH_LONG).show();

                                                    // CHANGED: Redirect to LoginActivity instead of MainActivity
                                                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                                    startActivity(intent);
                                                    finish(); // Closes the sign-up view
                                                }
                                            });
                                } else {
                                    Toast.makeText(SignUpActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}