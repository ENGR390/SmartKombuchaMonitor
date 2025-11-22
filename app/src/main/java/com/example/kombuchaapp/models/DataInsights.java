package com.example.kombuchaapp.models;

import java.util.HashMap;
import java.util.Map;

public class DataInsights {
    
    // Overall statistics
    private int totalRecipes;
    private int completedBrews;
    private float successRate;
    private float avgFermentationDays;
    
    // Temperature statistics
    private float avgTemperatureC;
    private float optimalTempRangeMin;
    private float optimalTempRangeMax;
    private int tempAlertsTriggered;
    
    // pH statistics
    private float avgPhAtHarvest;
    private float optimalPhRangeMin;
    private float optimalPhRangeMax;
    private float avgTimeToOptimalPhDays;
    
    // Recipe statistics
    private String mostUsedTea;
    private String mostSuccessfulRecipe;
    private Map<String, Integer> statusCounts;
    private Map<String, Integer> teaTypeCounts;
    
    public DataInsights() {
        statusCounts = new HashMap<>();
        teaTypeCounts = new HashMap<>();
    }

    // Getters and Setters
    public int getTotalRecipes() {
        return totalRecipes;
    }

    public void setTotalRecipes(int totalRecipes) {
        this.totalRecipes = totalRecipes;
    }

    public int getCompletedBrews() {
        return completedBrews;
    }

    public void setCompletedBrews(int completedBrews) {
        this.completedBrews = completedBrews;
    }

    public float getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(float successRate) {
        this.successRate = successRate;
    }

    public float getAvgFermentationDays() {
        return avgFermentationDays;
    }

    public void setAvgFermentationDays(float avgFermentationDays) {
        this.avgFermentationDays = avgFermentationDays;
    }

    public float getAvgTemperatureC() {
        return avgTemperatureC;
    }

    public void setAvgTemperatureC(float avgTemperatureC) {
        this.avgTemperatureC = avgTemperatureC;
    }

    public float getOptimalTempRangeMin() {
        return optimalTempRangeMin;
    }

    public void setOptimalTempRangeMin(float optimalTempRangeMin) {
        this.optimalTempRangeMin = optimalTempRangeMin;
    }

    public float getOptimalTempRangeMax() {
        return optimalTempRangeMax;
    }

    public void setOptimalTempRangeMax(float optimalTempRangeMax) {
        this.optimalTempRangeMax = optimalTempRangeMax;
    }

    public int getTempAlertsTriggered() {
        return tempAlertsTriggered;
    }

    public void setTempAlertsTriggered(int tempAlertsTriggered) {
        this.tempAlertsTriggered = tempAlertsTriggered;
    }

    public float getAvgPhAtHarvest() {
        return avgPhAtHarvest;
    }

    public void setAvgPhAtHarvest(float avgPhAtHarvest) {
        this.avgPhAtHarvest = avgPhAtHarvest;
    }

    public float getOptimalPhRangeMin() {
        return optimalPhRangeMin;
    }

    public void setOptimalPhRangeMin(float optimalPhRangeMin) {
        this.optimalPhRangeMin = optimalPhRangeMin;
    }

    public float getOptimalPhRangeMax() {
        return optimalPhRangeMax;
    }

    public void setOptimalPhRangeMax(float optimalPhRangeMax) {
        this.optimalPhRangeMax = optimalPhRangeMax;
    }

    public float getAvgTimeToOptimalPhDays() {
        return avgTimeToOptimalPhDays;
    }

    public void setAvgTimeToOptimalPhDays(float avgTimeToOptimalPhDays) {
        this.avgTimeToOptimalPhDays = avgTimeToOptimalPhDays;
    }

    public String getMostUsedTea() {
        return mostUsedTea;
    }

    public void setMostUsedTea(String mostUsedTea) {
        this.mostUsedTea = mostUsedTea;
    }

    public String getMostSuccessfulRecipe() {
        return mostSuccessfulRecipe;
    }

    public void setMostSuccessfulRecipe(String mostSuccessfulRecipe) {
        this.mostSuccessfulRecipe = mostSuccessfulRecipe;
    }

    public Map<String, Integer> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Integer> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public Map<String, Integer> getTeaTypeCounts() {
        return teaTypeCounts;
    }

    public void setTeaTypeCounts(Map<String, Integer> teaTypeCounts) {
        this.teaTypeCounts = teaTypeCounts;
    }

    @Override
    public String toString() {
        return "DataInsights{" +
                "totalRecipes=" + totalRecipes +
                ", completedBrews=" + completedBrews +
                ", successRate=" + successRate +
                ", avgFermentationDays=" + avgFermentationDays +
                ", avgTemperatureC=" + avgTemperatureC +
                ", avgPhAtHarvest=" + avgPhAtHarvest +
                ", mostUsedTea='" + mostUsedTea + '\'' +
                ", mostSuccessfulRecipe='" + mostSuccessfulRecipe + '\'' +
                '}';
    }
}
