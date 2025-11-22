package com.example.kombuchaapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.kombuchaapp.models.DataInsights;
import com.example.kombuchaapp.repositories.DataInsightsRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DataInsightsActivity extends AppCompatActivity {

    private static final String TAG = "DataInsightsActivity";

    // UI Components
    private ProgressBar progressBar;
    private CardView noDataCard, insightsCard;
    
    // Overall Stats
    private TextView tvTotalRecipes, tvCompletedBrews, tvSuccessRate, tvAvgFermentTime;
    
    // Temperature Stats
    private TextView tvAvgTemp, tvOptimalTempRange, tvTempAlertsCount;
    
    // pH Stats
    private TextView tvAvgPh, tvOptimalPhRange, tvAvgTimeToOptimalPh;
    
    // Recipe Stats
    private TextView tvMostUsedTea, tvMostSuccessfulRecipe;
    
    // Charts
    private PieChart statusPieChart;
    private BarChart teaTypesBarChart;
    
    private DataInsightsRepository insightsRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_insights);

        insightsRepository = new DataInsightsRepository();

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> 
            FizzTransitionUtil.play(DataInsightsActivity.this, this::finish)
        );

        initViews();
        setupCharts();
        loadInsights();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        noDataCard = findViewById(R.id.no_data_card);
        insightsCard = findViewById(R.id.insights_card);
        
        // Overall stats
        tvTotalRecipes = findViewById(R.id.tv_total_recipes);
        tvCompletedBrews = findViewById(R.id.tv_completed_brews);
        tvSuccessRate = findViewById(R.id.tv_success_rate);
        tvAvgFermentTime = findViewById(R.id.tv_avg_ferment_time);
        
        // Temperature stats
        tvAvgTemp = findViewById(R.id.tv_avg_temp);
        tvOptimalTempRange = findViewById(R.id.tv_optimal_temp_range);
        tvTempAlertsCount = findViewById(R.id.tv_temp_alerts_count);
        
        // pH stats
        tvAvgPh = findViewById(R.id.tv_avg_ph);
        tvOptimalPhRange = findViewById(R.id.tv_optimal_ph_range);
        tvAvgTimeToOptimalPh = findViewById(R.id.tv_avg_time_to_optimal_ph);
        
        // Recipe stats
        tvMostUsedTea = findViewById(R.id.tv_most_used_tea);
        tvMostSuccessfulRecipe = findViewById(R.id.tv_most_successful_recipe);
        
        // Charts
        statusPieChart = findViewById(R.id.status_pie_chart);
        teaTypesBarChart = findViewById(R.id.tea_types_bar_chart);
    }

    private void setupCharts() {
        // Setup Pie Chart
        statusPieChart.setUsePercentValues(true);
        statusPieChart.getDescription().setEnabled(false);
        statusPieChart.setDrawHoleEnabled(true);
        statusPieChart.setHoleColor(Color.WHITE);
        statusPieChart.setTransparentCircleRadius(61f);
        statusPieChart.setDrawEntryLabels(false);
        statusPieChart.getLegend().setEnabled(true);
        statusPieChart.setNoDataText("No recipe data available");
        
        // Setup Bar Chart
        teaTypesBarChart.getDescription().setEnabled(false);
        teaTypesBarChart.setDrawGridBackground(false);
        teaTypesBarChart.setDrawBorders(false);
        teaTypesBarChart.setTouchEnabled(true);
        teaTypesBarChart.setDragEnabled(true);
        teaTypesBarChart.setScaleEnabled(false);
        teaTypesBarChart.setPinchZoom(false);
        
        XAxis xAxis = teaTypesBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        
        YAxis leftAxis = teaTypesBarChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f);
        
        teaTypesBarChart.getAxisRight().setEnabled(false);
        teaTypesBarChart.getLegend().setEnabled(false);
        teaTypesBarChart.setNoDataText("No tea type data available");
    }

    private void loadInsights() {
        showLoading(true);
        
        insightsRepository.calculateInsights(new DataInsightsRepository.OnInsightsLoadedListener() {
            @Override
            public void onSuccess(DataInsights insights) {
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    if (insights.getTotalRecipes() == 0) {
                        showNoDataState();
                    } else {
                        displayInsights(insights);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(DataInsightsActivity.this,
                            "Error loading insights: " + error,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load insights: " + error);
                    showNoDataState();
                });
            }
        });
    }

    private void displayInsights(DataInsights insights) {
        noDataCard.setVisibility(View.GONE);
        insightsCard.setVisibility(View.VISIBLE);
        
        // Overall statistics
        tvTotalRecipes.setText(String.valueOf(insights.getTotalRecipes()));
        tvCompletedBrews.setText(String.valueOf(insights.getCompletedBrews()));
        tvSuccessRate.setText(String.format(Locale.getDefault(), "%.1f%%", insights.getSuccessRate()));
        
        if (insights.getAvgFermentationDays() > 0) {
            tvAvgFermentTime.setText(String.format(Locale.getDefault(), 
                "%.1f days", insights.getAvgFermentationDays()));
        } else {
            tvAvgFermentTime.setText("N/A");
        }
        
        // Temperature statistics
        if (insights.getAvgTemperatureC() > 0) {
            tvAvgTemp.setText(String.format(Locale.getDefault(), "%.1f°C", insights.getAvgTemperatureC()));
        } else {
            tvAvgTemp.setText("N/A");
        }
        
        if (insights.getOptimalTempRangeMin() > 0 && insights.getOptimalTempRangeMax() > 0) {
            tvOptimalTempRange.setText(String.format(Locale.getDefault(), 
                "%.1f°C - %.1f°C", insights.getOptimalTempRangeMin(), insights.getOptimalTempRangeMax()));
        } else {
            tvOptimalTempRange.setText("N/A");
        }
        
        tvTempAlertsCount.setText(String.valueOf(insights.getTempAlertsTriggered()));
        
        // pH statistics
        if (insights.getAvgPhAtHarvest() > 0) {
            tvAvgPh.setText(String.format(Locale.getDefault(), "%.2f", insights.getAvgPhAtHarvest()));
        } else {
            tvAvgPh.setText("N/A");
        }
        
        if (insights.getOptimalPhRangeMin() > 0 && insights.getOptimalPhRangeMax() > 0) {
            tvOptimalPhRange.setText(String.format(Locale.getDefault(), 
                "%.2f - %.2f", insights.getOptimalPhRangeMin(), insights.getOptimalPhRangeMax()));
        } else {
            tvOptimalPhRange.setText("2.5 - 3.5 (standard)");
        }
        
        if (insights.getAvgTimeToOptimalPhDays() > 0) {
            tvAvgTimeToOptimalPh.setText(String.format(Locale.getDefault(), 
                "%.1f days", insights.getAvgTimeToOptimalPhDays()));
        } else {
            tvAvgTimeToOptimalPh.setText("N/A");
        }
        
        // Recipe statistics
        if (insights.getMostUsedTea() != null && !insights.getMostUsedTea().isEmpty()) {
            tvMostUsedTea.setText(insights.getMostUsedTea());
        } else {
            tvMostUsedTea.setText("N/A");
        }
        
        if (insights.getMostSuccessfulRecipe() != null && !insights.getMostSuccessfulRecipe().isEmpty()) {
            tvMostSuccessfulRecipe.setText(insights.getMostSuccessfulRecipe());
        } else {
            tvMostSuccessfulRecipe.setText("N/A");
        }
        
        // Update charts
        updateStatusPieChart(insights);
        updateTeaTypesBarChart(insights);
    }

    private void updateStatusPieChart(DataInsights insights) {
        Map<String, Integer> statusCounts = insights.getStatusCounts();
        
        if (statusCounts.isEmpty()) {
            statusPieChart.setNoDataText("No status data available");
            statusPieChart.invalidate();
            return;
        }
        
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey().toUpperCase()));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "Recipe Status");
        
        // Set colors based on status
        List<Integer> colors = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
            switch (entry.getKey().toLowerCase()) {
                case "draft":
                    colors.add(Color.parseColor("#757575")); // Gray
                    break;
                case "brewing":
                    colors.add(Color.parseColor("#FF9100")); // Orange
                    break;
                case "paused":
                    colors.add(Color.parseColor("#FF9800")); // Amber
                    break;
                case "completed":
                    colors.add(Color.parseColor("#4CAF50")); // Green
                    break;
                default:
                    colors.add(Color.parseColor("#4A148C")); // Purple
                    break;
            }
        }
        dataSet.setColors(colors);
        
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(statusPieChart));
        
        statusPieChart.setData(data);
        statusPieChart.animateY(1000);
        statusPieChart.invalidate();
    }

    private void updateTeaTypesBarChart(DataInsights insights) {
        Map<String, Integer> teaCounts = insights.getTeaTypeCounts();
        
        if (teaCounts.isEmpty()) {
            teaTypesBarChart.setNoDataText("No tea type data available");
            teaTypesBarChart.invalidate();
            return;
        }
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        
        for (Map.Entry<String, Integer> entry : teaCounts.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Tea Types");
        dataSet.setColor(Color.parseColor("#FF8C00"));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#8B4513"));
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        
        teaTypesBarChart.setData(barData);
        teaTypesBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        teaTypesBarChart.getXAxis().setLabelCount(labels.size());
        teaTypesBarChart.animateY(1000);
        teaTypesBarChart.invalidate();
    }

    private void showNoDataState() {
        noDataCard.setVisibility(View.VISIBLE);
        insightsCard.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            noDataCard.setVisibility(View.GONE);
            insightsCard.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        FizzTransitionUtil.play(this, this::finish);
    }
}
