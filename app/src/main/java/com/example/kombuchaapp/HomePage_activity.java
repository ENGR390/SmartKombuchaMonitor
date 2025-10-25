package com.example.kombuchaapp;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Locale;
import java.util.Map;

public class HomePage_activity extends AppCompatActivity {

    private static final String TAG = "HomePage_activity";

    private ViewGroup listParent;
    private View cardTemplate;

    private FirebaseFirestore db;
    private ListenerRegistration recipesListener;

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
        btnBack.setOnClickListener(v -> finish());

        cardTemplate = findViewById(R.id.item_card);
        listParent = (ViewGroup) cardTemplate.getParent();
        cardTemplate.setVisibility(View.GONE);

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || u.getUid() == null || u.getUid().isEmpty()) {
            Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show();
            showStatusCard("Not signed in.");
            return;
        }

        String uid = u.getUid();
        Log.d(TAG, "uid=" + uid);

        startRecipesListener(uid);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recipesListener != null) {
            recipesListener.remove();
            recipesListener = null;
        }
    }

    private void startRecipesListener(String uid) {
        recipesListener = db.collection("users")
                .document(uid)
                .collection("Recipes")
                .addSnapshotListener((QuerySnapshot snaps, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.e(TAG, "Firestore error:", e);
                        showStatusCard("Cannot load recipes:\n" + e.getMessage());
                        return;
                    }

                    if (snaps == null) {
                        Log.d(TAG, "Snapshot null");
                        showStatusCard("Cannot load recipes.");
                        return;
                    }

                    if (snaps.isEmpty()) {
                        Log.d(TAG, "No recipes for uid " + uid);
                        showStatusCard("You have 0 recipes.");
                        return;
                    }

                    listParent.removeAllViews();

                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        String recipeId = doc.getId();
                        Map<String, Object> fields = doc.getData();
                        View card = buildRecipeCard(recipeId, fields);
                        listParent.addView(card, buildListItemLayoutParams());
                    }
                });
    }

    private View buildRecipeCard(String recipeId, Map<String, Object> fields) {
        LinearLayout card = makeBaseCard();

        TextView title = makeTitleText(recipeId);
        card.addView(title, matchWrap());

        LinearLayout details = makeDetailsContainer();
        card.addView(details, matchWrap());

        if (fields == null || fields.isEmpty()) {
            addDetailLine(details, "No fields", "—");
        } else {
            for (Map.Entry<String, Object> e : fields.entrySet()) {
                String key = prettyKey(e.getKey());
                String value = (e.getValue() == null) ? "null" : String.valueOf(e.getValue());
                addDetailLine(details, key, value);
            }
        }

        card.setOnClickListener(v -> {
            details.setVisibility(details.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        return card;
    }

    private void showStatusCard(String message) {
        listParent.removeAllViews();

        LinearLayout statusCard = makeBaseCard();

        TextView title = new TextView(this);
        title.setText(message);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(Color.parseColor("#1A1A1A"));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

        statusCard.addView(title, matchWrap());

        listParent.addView(statusCard, buildListItemLayoutParams());
    }

    private LinearLayout makeBaseCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.parseColor("#1F000000"));
        card.setBackground(bg);

        TypedValue out = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, out, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            card.setForeground(AppCompatResources.getDrawable(this, out.resourceId));
        } else {
            card.setClickable(true);
        }

        return card;
    }

    private TextView makeTitleText(String titleText) {
        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTextColor(Color.parseColor("#1A1A1A"));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        return title;
    }

    private LinearLayout makeDetailsContainer() {
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setVisibility(View.GONE);
        details.setPadding(0, dp(12), 0, 0);
        return details;
    }

    private void addDetailLine(LinearLayout container, String key, String value) {
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

        row.addView(k, matchWrap());
        row.addView(v, matchWrap());

        container.addView(row, matchWrap());

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#1F000000"));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        dlp.setMargins(0, dp(6), 0, dp(6));
        container.addView(divider, dlp);
    }

    private LinearLayout.LayoutParams buildListItemLayoutParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(dp(4), dp(6), dp(4), dp(6));
        return lp;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private static String prettyKey(String k) {
        if (k == null || k.isEmpty()) return "Field";
        String t = k.replace("_", " ").replace("-", " ");
        return t.substring(0, 1).toUpperCase(Locale.getDefault()) + t.substring(1);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}