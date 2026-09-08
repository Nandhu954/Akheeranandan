package com.campusfind.app.models;

import java.io.Serializable;

public class Claim implements Serializable {
    private int claimId;
    private int userId;
    private int itemId;
    private String itemType; // "Lost" or "Found"
    private String qrPayload;
    private String claimStatus; // "Pending", "Verified", "Returned"

    // Helper display fields
    private String itemName;
    private String claimerName;
    private String claimerRollNo;
    private String claimerEmail;

    public Claim() {
    }

    public Claim(int claimId, int userId, int itemId, String itemType, String qrPayload, String claimStatus) {
        this.claimId = claimId;
        this.userId = userId;
        this.itemId = itemId;
        this.itemType = itemType;
        this.qrPayload = qrPayload;
        this.claimStatus = claimStatus;
    }

    public int getClaimId() {
        return claimId;
    }

    public void setClaimId(int claimId) {
        this.claimId = claimId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public String getClaimStatus() {
        return claimStatus;
    }

    public void setClaimStatus(String claimStatus) {
        this.claimStatus = claimStatus;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getClaimerName() {
        return claimerName;
    }

    public void setClaimerName(String claimerName) {
        this.claimerName = claimerName;
    }

    public String getClaimerRollNo() {
        return claimerRollNo;
    }

    public void setClaimerRollNo(String claimerRollNo) {
        this.claimerRollNo = claimerRollNo;
    }

    public String getClaimerEmail() {
        return claimerEmail;
    }

    public void setClaimerEmail(String claimerEmail) {
        this.claimerEmail = claimerEmail;
    }
}
