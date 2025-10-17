package com.example.kombuchaapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


public class HomePage_activity extends AppCompatActivity {


    private Toolbar toolbar;
    private ImageButton btnBack;
    private TextView toolbarTitle;

    private RecyclerView recycler;
    private View emptyContainer;

    private final ItemsAdapter adapter = new ItemsAdapter();

    private FirebaseFirestore db;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage_activity);

        // Firebase
        FirebaseApp.initializeApp(getApplicationContext());
        db = FirebaseFirestore.getInstance();

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        btnBack = findViewById(R.id.btn_back);
        toolbarTitle = findViewById(R.id.toolbar_title);
        toolbar.setTitle("");
        if (btnBack.getDrawable() == null) {
            btnBack.setImageDrawable(AppCompatResources.getDrawable(
                    this, androidx.appcompat.R.drawable.abc_ic_ab_back_material));
        }
        btnBack.setContentDescription("Navigate back");
        btnBack.setOnClickListener(v -> finish());

        // List + empty state
        recycler = findViewById(R.id.recycler);
        emptyContainer = findViewById(R.id.empty_container);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        // Real-time listener: new items show immediately
        registration = db.collection("items")
                .orderBy("title", Query.Direction.ASCENDING) // change field if needed
                .addSnapshotListener((QuerySnapshot value, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Load failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;

                    List<Row> rows = new ArrayList<>();
                    for (DocumentSnapshot d : value.getDocuments()) {
                        String id = d.getId();
                        String title = getOrDefault(d.getString("title"), id);     // fallback to id
                        String subtitle = getOrDefault(d.getString("subtitle"), ""); // optional
                        rows.add(new Row(id, title, subtitle));
                    }
                    adapter.set(rows);
                    emptyContainer.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) { registration.remove(); registration = null; }
    }

    // ---------- Model ----------
    static class Row {
        final String id, title, subtitle;
        Row(String id, String title, String subtitle) { this.id = id; this.title = title; this.subtitle = subtitle; }
    }

    // ---------- Adapter: each item = its own LinearLayout "card" ----------
    class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.VH> {
        private final List<Row> data = new ArrayList<>();

        void set(List<Row> rows) {
            data.clear();
            data.addAll(rows);
            notifyDataSetChanged();
        }

        @Override public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            // Outer LinearLayout as the row container (card)
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            int padH = dp(16), padV = dp(12);
            card.setPadding(padH, padV, padH, padV);

            // Rounded white background with subtle stroke
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(16));
            bg.setStroke(dp(1), Color.parseColor("#1F000000"));
            card.setBackground(bg);

            // Ripple foreground for touch feedback
            TypedValue out = new TypedValue();
            parent.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, out, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                card.setForeground(AppCompatResources.getDrawable(parent.getContext(), out.resourceId));
            } else {
                card.setClickable(true);
            }

            // Title
            TextView title = new TextView(parent.getContext());
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            title.setTextColor(Color.parseColor("#1A1A1A"));
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            card.addView(title, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Subtitle
            TextView subtitle = new TextView(parent.getContext());
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            subtitle.setTextColor(Color.parseColor("#6B6B6B"));
            subtitle.setPadding(0, dp(4), 0, 0);
            card.addView(subtitle, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Row spacing/margins
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(4), dp(6), dp(4), dp(6));
            card.setLayoutParams(lp);

            return new VH(card, title, subtitle);
        }

        @Override public void onBindViewHolder(VH h, int position) {
            Row row = data.get(position);
            h.title.setText(row.title);
            if (row.subtitle == null || row.subtitle.trim().isEmpty()) {
                h.subtitle.setVisibility(View.GONE);
            } else {
                h.subtitle.setVisibility(View.VISIBLE);
                h.subtitle.setText(row.subtitle);
            }
            h.itemView.setOnClickListener(v -> openDetail(row));
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView title, subtitle;
            VH(View itemView, TextView title, TextView subtitle) {
                super(itemView);
                this.title = title;
                this.subtitle = subtitle;
            }
        }
    }

    // ---------- Opens the detailed page ----------
    private void openDetail(Row row) {
        //TODO
        // this is the function that opens the detailed page
    }

    // ---------- helpers ----------
    private static String getOrDefault(String v, String fallback) {
        if (v == null) return fallback;
        String t = v.trim();
        return t.isEmpty() ? fallback : t;
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
