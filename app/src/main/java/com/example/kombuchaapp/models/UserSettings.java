package com.example.kombuchaapp.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

// Data model for user settings stored in Firebase Firestore

public class UserSettings {
    
    private String userId;
    private String username;
    private String email;
    
    private String temperatureUnit;  //F or C
    private int fontSize;             
    private String themeColor;  
  
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Empty constructor required for Firestore
    public UserSettings() {
    }
    
    public UserSettings(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        
        this.temperatureUnit = "celsius";
        this.fontSize = 0; 
        this.themeColor = "purple";
        
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getTemperatureUnit() {
        return temperatureUnit;
    }
    
    public void setTemperatureUnit(String temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }
    
    public int getFontSize() {
        return fontSize;
    }
    
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }
    
    public String getThemeColor() {
        return themeColor;
    }
    
    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Convert UserSettings object to Map for Firestore

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("username", username);
        map.put("email", email);
        map.put("temperatureUnit", temperatureUnit);
        map.put("fontSize", fontSize);
        map.put("themeColor", themeColor);
        map.put("createdAt", createdAt);
        map.put("updatedAt", Timestamp.now());
        return map;
    }
    
    @Override
    public String toString() {
        return "UserSettings{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", temperatureUnit='" + temperatureUnit + '\'' +
                ", fontSize=" + fontSize +
                ", themeColor='" + themeColor + '\'' +
                '}';
    }
}
