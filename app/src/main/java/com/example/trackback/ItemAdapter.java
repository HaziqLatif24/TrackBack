package com.example.trackback;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class ItemAdapter extends ArrayAdapter<ItemReport> {

    public ItemAdapter(Context context, List<ItemReport> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemReport item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_row, parent, false);
        }

        ImageView ivRowThumb = convertView.findViewById(R.id.ivRowThumb);
        TextView tvRowTitle = convertView.findViewById(R.id.tvRowTitle);
        TextView tvRowTypeTag = convertView.findViewById(R.id.tvRowTypeTag);
        TextView tvRowLocation = convertView.findViewById(R.id.tvRowLocation);
        TextView tvRowDescription = convertView.findViewById(R.id.tvRowDescription);

        if (item != null) {
            tvRowTitle.setText(item.getTitle());
            tvRowDescription.setText(item.getDescription());

            // 1. Fetch current logged-in user ID safely
            String currentUserId = "";
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }

            // 2. Extract item metadata attributes from our unified model
            String originalStatus = item.getType();
            String postOwnerId = item.getCurrentUser(); // Points perfectly to getCurrentUser() now

            // 3. Dynamic Relative Status text rendering logic
            if (currentUserId.equals(postOwnerId)) {
                // If I am the person who posted it, show exactly what I selected ("LOST" or "FOUND")
                tvRowTypeTag.setText(originalStatus.toUpperCase());

                if ("Found".equalsIgnoreCase(originalStatus)) {
                    tvRowTypeTag.setTextColor(Color.parseColor("#1E7E34")); // Green for Found
                } else {
                    tvRowTypeTag.setTextColor(Color.parseColor("#D32F2F")); // Red for Lost
                }
            } else {
                // If SOMEONE ELSE is looking at this post:
                if ("Lost".equalsIgnoreCase(originalStatus)) {
                    // User A lost it -> User B sees it as standard LOST listing
                    tvRowTypeTag.setText("LOST");
                    tvRowTypeTag.setTextColor(Color.parseColor("#D32F2F"));
                } else {
                    // User A found it -> User B (the potential owner) sees it as a LOST target to claim
                    tvRowTypeTag.setText("LOST");
                    tvRowTypeTag.setTextColor(Color.parseColor("#D32F2F"));
                }
            }

            // Dynamic location pin view toggle
            if (originalStatus.equalsIgnoreCase("Lost") || (item.getLatitude() == 0.0 && item.getLongitude() == 0.0)) {
                tvRowLocation.setVisibility(View.GONE);
            } else {
                tvRowLocation.setVisibility(View.VISIBLE);
                tvRowLocation.setText("📍 Map Pinned Location");
            }

            // Decode the Base64 image text back into a visual image layout bitmap
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(item.getImageUrl(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivRowThumb.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    ivRowThumb.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                ivRowThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        return convertView;
    }
}