package com.example.trackback;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class DetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView tvDetailTitle, tvDetailDescription;
    private ImageView ivWhatsAppQR, ivDetailPhoto;
    private LinearLayout layoutDetailMapSection;
    private Button btnMarkCompleted, btnDetailBack;

    private double itemLatitude = 0.0;
    private double itemLongitude = 0.0;
    private String itemTitleStr = "";
    private String itemId = "";
    private androidx.cardview.widget.CardView cvDetailPhotoContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ivDetailPhoto = findViewById(R.id.ivDetailPhoto);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        layoutDetailMapSection = findViewById(R.id.layoutDetailMapSection);
        ivWhatsAppQR = findViewById(R.id.ivWhatsAppQR);
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted);
        btnDetailBack = findViewById(R.id.btnDetailBack);
        cvDetailPhotoContainer = findViewById(R.id.cvDetailPhotoContainer);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // Safe string extraction with fallback defaults prevents NullPointer crashes
            itemId = extras.getString("itemId", "");
            itemTitleStr = extras.getString("title", "No Title Provided");
            String description = extras.getString("description", "No description provided.");
            String contactPhone = extras.getString("contact", "");
            String type = extras.getString("type", "Lost");
            String imgUrl = extras.getString("imageUrl", "");
            String postUserId = extras.getString("postUserId", "");

            itemLatitude = extras.getDouble("latitude", 0.0);
            itemLongitude = extras.getDouble("longitude", 0.0);

            tvDetailTitle.setText(itemTitleStr + " (" + type.toUpperCase() + ")");
            tvDetailDescription.setText(description);

            // Hide map cleanly if item is lost or coordinates are default zero
            if (type.equalsIgnoreCase("Lost") || (itemLatitude == 0.0 && itemLongitude == 0.0)) {
                layoutDetailMapSection.setVisibility(View.GONE);
            } else {
                layoutDetailMapSection.setVisibility(View.VISIBLE);
            }

            // Verify Post Ownership logic boundaries safely
            String currentUserId = "";
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }

            // If the viewer is NOT the owner who posted this item, hide the complete button completely
            if (postUserId.isEmpty() || !postUserId.equals(currentUserId)) {
                btnMarkCompleted.setVisibility(View.GONE);
            } else {
                btnMarkCompleted.setVisibility(View.VISIBLE);
            }

            // Base64 decode string bitmap safe wrapper execution
            if (!imgUrl.isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(imgUrl, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (decodedByte != null) {
                        ivDetailPhoto.setImageBitmap(decodedByte);
                        cvDetailPhotoContainer.setVisibility(View.VISIBLE);
                    } else {
                        cvDetailPhotoContainer.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    cvDetailPhotoContainer.setVisibility(View.GONE);
                }
            } else {
                cvDetailPhotoContainer.setVisibility(View.GONE);
            }

            // Clean contact string formatting parsing wrapper
            if (!contactPhone.isEmpty()) {
                try {
                    String cleanedPhone = contactPhone.replaceAll("[\\s\\-+]", "");
                    if (cleanedPhone.startsWith("0")) {
                        cleanedPhone = "60" + cleanedPhone.substring(1);
                    }
                    String whatsappUrl = "https://wa.me/" + cleanedPhone;
                    Bitmap qrBitmap = generateQRCode(whatsappUrl);
                    if (qrBitmap != null) {
                        ivWhatsAppQR.setImageBitmap(qrBitmap);
                        ivWhatsAppQR.setVisibility(View.VISIBLE);
                    } else {
                        ivWhatsAppQR.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    ivWhatsAppQR.setVisibility(View.GONE);
                }
            } else {
                ivWhatsAppQR.setVisibility(View.GONE);
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.detailMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnMarkCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemId != null && !itemId.isEmpty()) {
                    DatabaseReference mItemRef = FirebaseDatabase.getInstance().getReference("items").child(itemId);
                    mItemRef.removeValue();
                    Toast.makeText(DetailActivity.this, "Activity resolved and removed from dashboard!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        btnDetailBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        if (itemLatitude != 0.0 && itemLongitude != 0.0) {
            LatLng pinnedLocation = new LatLng(itemLatitude, itemLongitude);
            googleMap.addMarker(new MarkerOptions().position(pinnedLocation).title(itemTitleStr));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pinnedLocation, 16.0f));
        }
    }

    private Bitmap generateQRCode(String text) {
        if (text == null || text.isEmpty()) return null;
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 400, 400);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            return null;
        }
    }
}