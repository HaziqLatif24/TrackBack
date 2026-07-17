package com.example.trackback;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ReportActivity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText etItemTitle, etDescription, etContact;
    private RadioGroup rgReportType;
    private LinearLayout layoutMapSection;
    private Button btnSelectImage, btnSubmitReport;
    private ImageView ivReportPreview;
    private ImageButton btnBackReport;

    private DatabaseReference mDatabase;
    private GoogleMap mGoogleMap;

    // NEW ADDITIONS: Location Client Provider for real-time tracking operations
    private FusedLocationProviderClient fusedLocationClient;

    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private Bitmap selectedBitmap = null;

    private Uri cameraImageUri = null; // Tracks our secure file pointer path

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001; // Core reference request tag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mDatabase = FirebaseDatabase.getInstance().getReference("items");

        // NEW ADDITIONS: Instantiating services pipeline
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        rgReportType = findViewById(R.id.rgReportType);
        layoutMapSection = findViewById(R.id.layoutMapSection);
        etItemTitle = findViewById(R.id.etItemTitle);
        etDescription = findViewById(R.id.etDescription);
        etContact = findViewById(R.id.etContact);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivReportPreview = findViewById(R.id.ivReportPreview);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        btnBackReport = findViewById(R.id.btnBackReport);

        btnBackReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.reportMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        rgReportType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbFound) {
                    layoutMapSection.setVisibility(View.VISIBLE);
                    // NEW ADDITIONS: Automatically fetch phone's position when switching view profiles
                    requestDeviceCurrentLocation();
                } else {
                    layoutMapSection.setVisibility(View.GONE);
                    selectedLatitude = 0.0;
                    selectedLongitude = 0.0;
                    if (mGoogleMap != null) {
                        mGoogleMap.clear();
                    }
                }
            }
        });

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSelectionDialog();
            }
        });

        btnSubmitReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveReportToDatabase();
            }
        });
    }

    private void showImageSelectionDialog() {
        String[] options = {"Take Photo via Camera", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Attachment Source");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    try {
                        java.io.File photoFile = java.io.File.createTempFile(
                                "cam_capture_",
                                ".jpg",
                                getCacheDir()
                        );

                        cameraImageUri = Uri.fromFile(photoFile);
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                        Uri secureProviderUri = androidx.core.content.FileProvider.getUriForFile(
                                ReportActivity.this,
                                "com.example.trackback.fileprovider",
                                photoFile
                        );

                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, secureProviderUri);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(ReportActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, PICK_IMAGE_REQUEST);
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                Uri targetUri = null;

                if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                    targetUri = data.getData();
                } else if (requestCode == CAMERA_REQUEST) {
                    targetUri = cameraImageUri;
                }

                if (targetUri != null) {
                    InputStream imageStream = getContentResolver().openInputStream(targetUri);
                    Bitmap originalBitmap = BitmapFactory.decodeStream(imageStream);

                    selectedBitmap = resizeBitmap(originalBitmap, 1024);
                    ivReportPreview.setImageBitmap(selectedBitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image assets", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void saveReportToDatabase() {
        String title = etItemTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String contact = etContact.getText().toString().trim();

        String type = "Lost";
        if (rgReportType.getCheckedRadioButtonId() == R.id.rbFound) {
            type = "Found";
        }

        if (title.isEmpty() || desc.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_LONG).show();
            return;
        }

        if (type.equals("Found") && selectedLatitude == 0.0 && selectedLongitude == 0.0) {
            Toast.makeText(this, "Please tap on the map to pin the location!", Toast.LENGTH_LONG).show();
            return;
        }

        String currentUserId = "";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        Toast.makeText(this, "Publishing... Please wait", Toast.LENGTH_SHORT).show();

        String base64Image = "";
        if (selectedBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] b = baos.toByteArray();
            base64Image = Base64.encodeToString(b, Base64.DEFAULT);
        }

        String itemId = mDatabase.push().getKey();
        ItemReport report = new ItemReport(itemId, title, desc, contact, type, selectedLatitude, selectedLongitude, base64Image, currentUserId);

        if (itemId != null) {
            mDatabase.child(itemId).setValue(report);
            Toast.makeText(this, "Item published online successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap;

        // Default viewpoint coordinates pointing around town campus center landmarks
        LatLng uitmShahAlam = new LatLng(3.0697, 101.5037);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uitmShahAlam, 15.0f));

        // Manual Map Click Override capability remains active completely
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                mGoogleMap.clear();
                selectedLatitude = latLng.latitude;
                selectedLongitude = latLng.longitude;
                mGoogleMap.addMarker(new MarkerOptions().position(latLng).title("Item Spot Location"));
            }
        });

        // Trigger automatic tracking directly if user default initialized with R.id.rbFound checked
        if (rgReportType.getCheckedRadioButtonId() == R.id.rbFound) {
            requestDeviceCurrentLocation();
        }
    }

    // NEW ADDITIONS: Automated GPS Tracker Engine implementation routine
    private void requestDeviceCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Pop up native platform permissions approval workflow checks cleanly
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        if (mGoogleMap != null) {
            mGoogleMap.setMyLocationEnabled(true); // Drops native interactive blue spot point view

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<android.location.Location>() {
                @Override
                public void onSuccess(android.location.Location location) {
                    if (location != null) {
                        LatLng realTimeLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        mGoogleMap.clear(); // Wipe placeholders clean
                        mGoogleMap.addMarker(new MarkerOptions().position(realTimeLatLng).title("Current Location (Auto Pinned)"));
                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(realTimeLatLng, 16.0f));

                        // Assign structural coordinate metrics values safely for background upload targets
                        selectedLatitude = location.getLatitude();
                        selectedLongitude = location.getLongitude();
                    } else {
                        Toast.makeText(ReportActivity.this, "GPS signal weak. Tap manually if required.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // NEW ADDITIONS: Safe permissions intercept handler callback loop
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestDeviceCurrentLocation();
            } else {
                Toast.makeText(this, "Permission denied. Manual map pin entry active.", Toast.LENGTH_LONG).show();
            }
        }
    }
}