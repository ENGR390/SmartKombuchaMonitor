package com.example.kombuchaapp;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Locale;
import java.util.Map;

public class HomePage_activity extends AppCompatActivity {

    private static final String DEFAULT_DOC_PATH = "users/demo/Items/demoItem";

    private View cardContainer;
    private TextView tvItemName;
    private LinearLayout detailsContainer;
    private View emptyState;

    private FirebaseFirestore db;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        FirebaseApp.initializeApp(getApplicationContext());
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbar.setTitle("");
        if (btnBack.getDrawable() == null) {
            btnBack.setImageDrawable(AppCompatResources.getDrawable(
                    this, androidx.appcompat.R.drawable.abc_ic_ab_back_material));
        }
        btnBack.setContentDescription("Navigate back");
        btnBack.setOnClickListener(v -> finish());

        cardContainer = findViewById(R.id.item_card);
        tvItemName = findViewById(R.id.item_name);
        detailsContainer = findViewById(R.id.details_container);
        emptyState = findViewById(R.id.empty_container);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.parseColor("#1F000000"));
        cardContainer.setBackground(bg);
        TypedValue out = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, out, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cardContainer.setForeground(AppCompatResources.getDrawable(this, out.resourceId));
        } else {
            cardContainer.setClickable(true);
        }

        String explicitDocPath = getIntent().getStringExtra("docPath");
        String explicitItemId  = getIntent().getStringExtra("itemId");

        if (explicitDocPath != null && !explicitDocPath.trim().isEmpty()) {
            attachListenerToDoc(explicitDocPath);
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getUid() != null && !user.getUid().isEmpty()) {
                String uid = user.getUid();
                if (explicitItemId != null && !explicitItemId.trim().isEmpty()) {
                    attachListenerToDoc("users/" + uid + "/Items/" + explicitItemId.trim());
                } else {
                    db.collection("users").document(uid).collection("Items")
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                                    String docId = task.getResult().getDocuments().get(0).getId();
                                    attachListenerToDoc("users/" + uid + "/Items/" + docId);
                                } else {
                                    db.collection("users").document(uid).collection("Items")
                                            .limit(1)
                                            .get()
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful() && task2.getResult() != null && !task2.getResult().isEmpty()) {
                                                    String docId = task2.getResult().getDocuments().get(0).getId();
                                                    attachListenerToDoc("users/" + uid + "/Items/" + docId);
                                                } else {
                                                    attachListenerToDoc(DEFAULT_DOC_PATH);
                                                }
                                            });
                                }
                            });
                }
            } else {
                attachListenerToDoc(DEFAULT_DOC_PATH);
            }
        }

        cardContainer.setOnClickListener(v -> {
            detailsContainer.setVisibility(
                    detailsContainer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) { registration.remove(); registration = null; }
    }

    private void attachListenerToDoc(String docPath) {
        if (registration != null) { registration.remove(); registration = null; }
        registration = db.document(docPath).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Toast.makeText(this, "Load failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty(true);
                return;
            }
            if (snapshot == null || !snapshot.exists()) {
                showEmpty(true);
                return;
            }
            showEmpty(false);
            bindDocument(snapshot);
        });
    }

    private void showEmpty(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        cardContainer.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void bindDocument(DocumentSnapshot doc) {
        tvItemName.setText(doc.getId()); // show the item name as the document ID
        detailsContainer.removeAllViews();
        Map<String, Object> map = doc.getData();
        if (map == null || map.isEmpty()) {
            addDetailLine("No fields", "—");
            return;
        }
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            addDetailLine(prettyKey(key), val == null ? "null" : String.valueOf(val));
        }
    }

    private void addDetailLine(String key, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(8);
        row.setPadding(0, pad, 0, pad);

        TextView k = new TextView(this);
        k.setText(key);
        k.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        k.setTextColor(Color.parseColor("#6B6B6B"));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        v.setTextColor(Color.parseColor("#1A1A1A"));

        row.addView(k, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(v, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        detailsContainer.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#1F000000"));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dlp.setMargins(0, dp(6), 0, dp(6));
        detailsContainer.addView(divider, dlp);
    }

    private static String prettyKey(String k) {
        if (k == null || k.isEmpty()) return "Field";
        String t = k.replace("_", " ").replace("-", " ");
        return t.substring(0, 1).toUpperCase(Locale.getDefault()) + t.substring(1);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}