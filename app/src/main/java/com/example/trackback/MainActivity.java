package com.example.trackback;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private Button btnCreateReport;
    private ImageButton btnProfile, btnLogout;

    // Separate UI list hooks and data sets
    private ListView lvLostItems, lvFoundItems;
    private List<ItemReport> lostItemList, foundItemList;
    private ItemAdapter lostAdapter, foundAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference("items");

        // View Bindings
        btnCreateReport = findViewById(R.id.btnCreateReport);
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);
        lvLostItems = findViewById(R.id.lvLostItems);
        lvFoundItems = findViewById(R.id.lvFoundItems);

        // Instantiating elements for Lost Section Feed
        lostItemList = new ArrayList<>();
        lostAdapter = new ItemAdapter(this, lostItemList);
        lvLostItems.setAdapter(lostAdapter);

        // Instantiating elements for Found Section Feed
        foundItemList = new ArrayList<>();
        foundAdapter = new ItemAdapter(this, foundItemList);
        lvFoundItems.setAdapter(foundAdapter);

        // 1. DATA SNAPSHOT FILTER DISTRIBUTOR LOOP
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lostItemList.clear();
                foundItemList.clear();

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    ItemReport item = postSnapshot.getValue(ItemReport.class);
                    if (item != null) {
                        // Split based on the item tag status attribute
                        if ("Found".equalsIgnoreCase(item.getType())) {
                            foundItemList.add(item);
                        } else {
                            lostItemList.add(item);
                        }
                    }
                }

                // Synchronize both list views updates instantly
                lostAdapter.notifyDataSetChanged();
                foundAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Database update error", Toast.LENGTH_SHORT).show();
            }
        });

        // Click actions for the list item view details mapping redirection
        setupListClickRedirection(lvLostItems, lostItemList);
        setupListClickRedirection(lvFoundItems, foundItemList);

        // NAVIGATION ACTIONS
        btnCreateReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportActivity.class));
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            }
        });

        // LOGOUT ACTION
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(MainActivity.this, "Logged out safely!", Toast.LENGTH_SHORT).show();

                // Returns user securely back into authentication interface
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(loginIntent);
                finish();
            }
        });
    }

    private void setupListClickRedirection(ListView listView, final List<ItemReport> dataSourceList) {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ItemReport clickedItem = dataSourceList.get(position);

                if (clickedItem != null) {
                    Intent intent = new Intent(MainActivity.this, DetailActivity.class);

                    // Safe null checks prevent NullPointerException crashes
                    intent.putExtra("itemId", clickedItem.getItemId() != null ? clickedItem.getItemId() : "");
                    intent.putExtra("title", clickedItem.getTitle() != null ? clickedItem.getTitle() : "No Title");
                    intent.putExtra("description", clickedItem.getDescription() != null ? clickedItem.getDescription() : "No Description");
                    intent.putExtra("contact", clickedItem.getContact() != null ? clickedItem.getContact() : "");
                    intent.putExtra("type", clickedItem.getType() != null ? clickedItem.getType() : "Lost");
                    intent.putExtra("latitude", clickedItem.getLatitude());
                    intent.putExtra("longitude", clickedItem.getLongitude());
                    intent.putExtra("imageUrl", clickedItem.getImageUrl() != null ? clickedItem.getImageUrl() : "");
                    intent.putExtra("postUserId", clickedItem.getCurrentUser() != null ? clickedItem.getCurrentUser() : "");

                    startActivity(intent);
                }
            }
        });
    }
}