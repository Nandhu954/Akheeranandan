package com.campusfind.app.models;

import java.io.Serializable;

public class Item implements Serializable {
    private int itemId;
    private int userId;
    private String itemType; // "Lost" or "Found"
    private String itemName;
    private String category;
    private String description;
    private String location; // location_lost or location_found
    private String date;     // date_lost or date_found
    private String status;   // "Active", "Matched", "Returned"
    private String photoPath; // local file path for Found item photo

    // Optional reporter details fetched via JOIN
    private String reporterName;
    private String reporterEmail;
    private String reporterRollNo;

    public Item() {
    }

    public Item(int itemId, int userId, String itemType, String itemName, String category,
                String description, String location, String date, String status) {
        this.itemId = itemId;
        this.userId = userId;
        this.itemType = itemType;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.location = location;
        this.date = date;
        this.status = status;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterEmail() {
        return reporterEmail;
    }

    public void setReporterEmail(String reporterEmail) {
        this.reporterEmail = reporterEmail;
    }

    public String getReporterRollNo() {
        return reporterRollNo;
    }

    public void setReporterRollNo(String reporterRollNo) {
        this.reporterRollNo = reporterRollNo;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
}
