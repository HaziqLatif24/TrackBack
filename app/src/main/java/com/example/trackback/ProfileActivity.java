package com.example.trackback;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileEmail;
    private Button btnEditProfile;
    private ImageButton btnBackProfile;
    private ListView profileItemListView;

    private DatabaseReference mUserDatabase, mItemDatabase;
    private String currentUserId;

    private List<ItemReport> userPostsList;
    private ItemAdapter profileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnBackProfile = findViewById(R.id.btnBackProfile);
        profileItemListView = findViewById(R.id.profileItemListView);

        userPostsList = new ArrayList<>();
        profileAdapter = new ItemAdapter(this, userPostsList);
        profileItemListView.setAdapter(profileAdapter);

        btnBackProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            tvProfileEmail.setText("Email: " + FirebaseAuth.getInstance().getCurrentUser().getEmail());
        }

        mUserDatabase = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        mItemDatabase = FirebaseDatabase.getInstance().getReference("items");

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = "No name found";

                    if (snapshot.child("fullName").getValue() != null) {
                        name = snapshot.child("fullName").getValue().toString();
                    } else if (snapshot.child("name").getValue() != null) {
                        name = snapshot.child("name").getValue().toString();
                    }

                    tvProfileName.setText("Name: " + name);
                } else {
                    tvProfileName.setText("Name: Account Data Missing");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        loadUserPosts();

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        profileItemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ItemReport targetItem = userPostsList.get(position);
                showEditPostDialog(targetItem);
            }
        });
    }

    private void loadUserPosts() {
        mItemDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userPostsList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    ItemReport post = postSnapshot.getValue(ItemReport.class);
                    if (post != null && currentUserId.equals(post.getCurrentUser())) {
                        userPostsList.add(post);
                    }
                }
                profileAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // UPDATED: Dialog now accepts Name and Password changes simultaneously
    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Profile Details");

        // Container structure to hold multiple input fields nicely
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 30);

        // Name Field Hook Setup
        TextView tvNameLabel = new TextView(this);
        tvNameLabel.setText("Full Name:");
        tvNameLabel.setTextColor(android.graphics.Color.BLACK);
        layout.addView(tvNameLabel);

        final EditText inputName = new EditText(this);
        inputName.setHint("Enter registered full name");
        // Pre-fill the input field with the text currently shown in profile header
        String currentNameText = tvProfileName.getText().toString().replace("Name: ", "").trim();
        if (!currentNameText.equals("Account Data Missing") && !currentNameText.equals("No name found")) {
            inputName.setText(currentNameText);
        }
        inputName.setLayoutParams(params);
        layout.addView(inputName);

        // Password Field Hook Setup
        TextView tvPassLabel = new TextView(this);
        tvPassLabel.setText("New Password (Leave blank to keep current):");
        tvPassLabel.setTextColor(android.graphics.Color.BLACK);
        layout.addView(tvPassLabel);

        final EditText inputPassword = new EditText(this);
        inputPassword.setHint("Enter new security password");
        inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inputPassword.setLayoutParams(params);
        layout.addView(inputPassword);

        builder.setView(layout);

        builder.setPositiveButton("Save Changes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = inputName.getText().toString().trim();
                final String newPassword = inputPassword.getText().toString().trim();

                // 1. Validate and Update Display Name
                if (newName.length() > 0) {
                    mUserDatabase.child("fullName").setValue(newName);
                    Toast.makeText(ProfileActivity.this, "Display name synchronized!", Toast.LENGTH_SHORT).show();
                }

                // 2. Process Password update if fields were supplied
                if (newPassword.length() > 0) {
                    if (newPassword.length() < 6) {
                        Toast.makeText(ProfileActivity.this, "Password update failed: Must be at least 6 characters.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(ProfileActivity.this, "Password security updated successfully!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Triggers if session expired (Requires logging out and back in)
                                            Toast.makeText(ProfileActivity.this, "Security Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditPostDialog(final ItemReport item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Posting Specifications");

        LinearLayout contextLayout = new LinearLayout(this);
        contextLayout.setOrientation(LinearLayout.VERTICAL);
        contextLayout.setPadding(50, 40, 50, 40);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 30);

        TextView tvTitleLabel = new TextView(this);
        tvTitleLabel.setText("Item Name:");
        tvTitleLabel.setTextColor(android.graphics.Color.BLACK);
        contextLayout.addView(tvTitleLabel);

        final EditText editTitle = new EditText(this);
        editTitle.setText(item.getTitle());
        editTitle.setLayoutParams(params);
        contextLayout.addView(editTitle);

        TextView tvDescLabel = new TextView(this);
        tvDescLabel.setText("Description:");
        tvDescLabel.setTextColor(android.graphics.Color.BLACK);
        contextLayout.addView(tvDescLabel);

        final EditText editDesc = new EditText(this);
        editDesc.setText(item.getDescription());
        editDesc.setLayoutParams(params);
        contextLayout.addView(editDesc);

        TextView tvContactLabel = new TextView(this);
        tvContactLabel.setText("Phone Number:");
        tvContactLabel.setTextColor(android.graphics.Color.BLACK);
        contextLayout.addView(tvContactLabel);

        final EditText editContact = new EditText(this);
        editContact.setText(item.getContact());
        editContact.setLayoutParams(params);
        contextLayout.addView(editContact);

        builder.setView(contextLayout);

        builder.setNeutralButton("Delete Post", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AlertDialog.Builder(ProfileActivity.this)
                        .setTitle("Delete Posting confirmation")
                        .setMessage("Are you absolutely sure you want to permanently delete this report entry?")
                        .setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mItemDatabase.child(item.getItemId()).removeValue();
                                Toast.makeText(ProfileActivity.this, "Post deleted successfully", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        builder.setPositiveButton("Save Post Changes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String updatedTitle = editTitle.getText().toString().trim();
                String updatedDesc = editDesc.getText().toString().trim();
                String updatedContact = editContact.getText().toString().trim();

                if (updatedTitle.length() > 0 && updatedDesc.length() > 0 && updatedContact.length() > 0) {
                    DatabaseReference targetPostRef = mItemDatabase.child(item.getItemId());
                    targetPostRef.child("title").setValue(updatedTitle);
                    targetPostRef.child("description").setValue(updatedDesc);
                    targetPostRef.child("contact").setValue(updatedContact);
                    Toast.makeText(ProfileActivity.this, "Post variations successfully modified!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this, "Fields cannot remain empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}