package com.example.kombuchaapp.models;

/**
 * User Settings Model
 * Extends the existing user document in Firestore: users/{userId}
 * Works with the same UID from Firebase Auth login system
 */
public class UserSettings {
    

    private String userId;
    private String fName;
    private String email;
    

    private String temperatureUnit;  
    private int fontSize;            
    private String themeColor;       

    // Empty constructor required for Firestore deserialization
    public UserSettings() {
        this.temperatureUnit = "celsius";
        this.fontSize = 16;
        this.themeColor = "purple";
    }


    public UserSettings(String userId, String fName, String email, 
                       String temperatureUnit, int fontSize, String themeColor) {
        this.userId = userId;
        this.fName = fName;
        this.email = email;
        this.temperatureUnit = temperatureUnit;
        this.fontSize = fontSize;
        this.themeColor = themeColor;
    }

    public String getUserId() { return userId; }
    public String getfName() { return fName; }
    public String getEmail() { return email; }
    public String getTemperatureUnit() { return temperatureUnit; }
    public int getFontSize() { return fontSize; }
    public String getThemeColor() { return themeColor; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setfName(String fName) { this.fName = fName; }
    public void setEmail(String email) { this.email = email; }
    public void setTemperatureUnit(String temperatureUnit) { this.temperatureUnit = temperatureUnit; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
    public void setThemeColor(String themeColor) { this.themeColor = themeColor; }

    @Override
    public String toString() {
        return "UserSettings{" +
                "userId='" + userId + '\'' +
                ", fName='" + fName + '\'' +
                ", email='" + email + '\'' +
                ", temperatureUnit='" + temperatureUnit + '\'' +
                ", fontSize=" + fontSize +
                ", themeColor='" + themeColor + '\'' +
                '}';
    }
}
