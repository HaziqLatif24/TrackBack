package com.example.trackback;

public class ItemReport {
    private String itemId;
    private String title;
    private String description;
    private String contact;
    private String type;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private String currentUser; // Tracks the ID of the user who posted it

    // Default constructor required for Firebase Realtime Database object mapping
    public ItemReport() {
    }

    // Main constructor matching ReportActivity initialization parameters
    public ItemReport(String itemId, String title, String description, String contact, String type, double latitude, double longitude, String imageUrl, String currentUser) {
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.contact = contact;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.currentUser = currentUser;
    }

    public String getItemId() { return itemId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getContact() { return contact; }
    public String getType() { return type; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getImageUrl() { return imageUrl; }

    // Unified getter used by the Adapter to identify the author
    public String getCurrentUser() { return currentUser; }
}