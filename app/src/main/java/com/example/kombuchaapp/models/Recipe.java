package com.example.kombuchaapp.models;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class Recipe {

    private String recipeId;
    private String userId;
    private String recipeName;

    // First Fermentation Ingredients
    private String teaLeaf;
    private String water;
    private String sugar;
    private String scoby;
    private String kombuchaStarter;

    // Second Fermentation
    private String flavor;

    // Metadata
    private Timestamp createdDate;
    private Timestamp brewingStartDate;
    private Timestamp completionDate;
    private String status;
    private String notes;
    private double minPh;
    private double maxPh;
    
    private Boolean published;

    private int likes = 0;

    private List<String> likedBy = new ArrayList<>();

    // Review fields
    private Float rating;
    private String reviewNotes;
    private Timestamp reviewDate;


    public Recipe() {
        this.status = "draft";
        this.createdDate = Timestamp.now();
    }

    public Recipe(String userId, String recipeName, String teaLeaf, String water,
                  String sugar, String scoby, String kombuchaStarter, String flavor, double minPh, double maxPh) {
        this.userId = userId;
        this.recipeName = recipeName;
        this.teaLeaf = teaLeaf;
        this.water = water;
        this.sugar = sugar;
        this.scoby = scoby;
        this.kombuchaStarter = kombuchaStarter;
        this.flavor = flavor;
        this.minPh = minPh;
        this.maxPh = maxPh;
        this.status = "draft";
        this.createdDate = Timestamp.now();
        this.published = false;
    }
  
    public String getRecipeId() { return recipeId; }
    public String getUserId() { return userId; }
    public String getRecipeName() { return recipeName; }
    public String getTeaLeaf() { return teaLeaf; }
    public String getWater() { return water; }
    public String getSugar() { return sugar; }
    public String getScoby() { return scoby; }
    public String getKombuchaStarter() { return kombuchaStarter; }
    public String getFlavor() { return flavor; }
    public Timestamp getCreatedDate() { return createdDate; }
    public Timestamp getBrewingStartDate() { return brewingStartDate; }
    public Timestamp getCompletionDate() { return completionDate; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public Boolean getPublished() {return published; }
    public int getLikes() { return likes; }
    public List<String> getLikedBy() { return likedBy; }
    public Float getRating() { return rating; }
    public String getReviewNotes() { return reviewNotes; }
    public Timestamp getReviewDate() { return reviewDate; }

    public void setLikedBy(List<String> likedBy) {this.likedBy = likedBy; }

    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setRecipeName(String recipeName) { this.recipeName = recipeName; }
    public void setTeaLeaf(String teaLeaf) { this.teaLeaf = teaLeaf; }
    public void setWater(String water) { this.water = water; }
    public void setSugar(String sugar) { this.sugar = sugar; }
    public void setScoby(String scoby) { this.scoby = scoby; }
    public void setKombuchaStarter(String kombuchaStarter) { this.kombuchaStarter = kombuchaStarter; }
    public void setFlavor(String flavor) { this.flavor = flavor; }
    public void setCreatedDate(Timestamp createdDate) { this.createdDate = createdDate; }
    public void setBrewingStartDate(Timestamp brewingStartDate) { this.brewingStartDate = brewingStartDate; }
    public void setCompletionDate(Timestamp completionDate) { this.completionDate = completionDate; }
    public void setStatus(String status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public double getMinPh() {
        return minPh;
    }
    public void setMinPh(double minPh) {
        this.minPh = minPh;
    }
    public double getMaxPh() {
        return maxPh;
    }
    public void setMaxPh(double maxPh) {
        this.maxPh = maxPh;
    }
    public void setPublished(Boolean published ) { this.published = published; }
    public void setLikes(int likes) { this.likes = likes; }
    public void setRating(Float rating) { this.rating = rating; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
    public void setReviewDate(Timestamp reviewDate) { this.reviewDate = reviewDate; }

    @Override
    public String toString() {
        return  "Recipe{" +
                "recipeId='" + recipeId + '\'' +
                ", userId='" + userId + '\'' +
                ", recipeName='" + recipeName + '\'' +
                ", teaLeaf='" + teaLeaf + '\'' +
                ", water='" + water + '\'' +
                ", sugar='" + sugar + '\'' +
                ", scoby='" + scoby + '\'' +
                ", kombuchaStarter='" + kombuchaStarter + '\'' +
                ", flavor='" + flavor + '\'' +
                ", status='" + status + '\'' +
                ", createdDate=" + createdDate + '\'' +
                ", published= "+ published + '\'' +
                ", likes=" + likes + '\'' +
                ", likedBy=" + likedBy + '\'' +
                ", rating=" + rating + '\'' +
                ", reviewNotes='" + reviewNotes + '\'' +
                '}';
    }
}
