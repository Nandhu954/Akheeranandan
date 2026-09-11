package com.campusfind.app.utils;

import android.util.Log;

import com.campusfind.app.models.Item;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FirestoreHelper — singleton that handles all cloud (Firestore) reads and writes.
 *
 * Collections:
 *   lost_items  — every lost-item report
 *   found_items — every found-item report
 *
 * Usage:
 *   FirestoreHelper.getInstance().saveLostItem(item, callback);
 *   FirestoreHelper.getInstance().getLostItems(callback);
 */
public class FirestoreHelper {

    private static final String TAG       = "FirestoreHelper";
    private static final String COL_LOST  = "lost_items";
    private static final String COL_FOUND = "found_items";

    private static FirestoreHelper instance;
    private final FirebaseFirestore db;

    // ─── Callback interfaces ──────────────────────────────────────────────────

    public interface ItemsCallback {
        void onSuccess(List<Item> items);
        void onError(String message);
    }

    public interface SaveCallback {
        void onSuccess(String firestoreId);
        void onError(String message);
    }

    // ─── Singleton ────────────────────────────────────────────────────────────

    private FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreHelper getInstance() {
        if (instance == null) {
            instance = new FirestoreHelper();
        }
        return instance;
    }

    // ─── SAVE ─────────────────────────────────────────────────────────────────

    /** Save a lost-item report to Firestore so all users can see it. */
    public void saveLostItem(Item item, SaveCallback callback) {
        db.collection(COL_LOST)
                .add(buildMap(item))
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Lost item saved to cloud: " + docRef.getId());
                    item.setFirestoreId(docRef.getId());
                    if (callback != null) callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveLostItem failed: " + e.getMessage());
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /** Save a found-item report to Firestore so all users can see it. */
    public void saveFoundItem(Item item, SaveCallback callback) {
        db.collection(COL_FOUND)
                .add(buildMap(item))
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Found item saved to cloud: " + docRef.getId());
                    item.setFirestoreId(docRef.getId());
                    if (callback != null) callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveFoundItem failed: " + e.getMessage());
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    // ─── FETCH ────────────────────────────────────────────────────────────────

    /**
     * Fetch ALL lost items from Firestore (active + matched, not returned).
     * Filtering by search query / category is done client-side in SearchActivity.
     */
    public void getLostItems(ItemsCallback callback) {
        db.collection(COL_LOST)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Item> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Item item = docToItem(doc, "Lost");
                        // Skip "Returned" items — they're no longer needed
                        if (!"Returned".equals(item.getStatus())) {
                            items.add(item);
                        }
                    }
                    Log.d(TAG, "Fetched " + items.size() + " lost items from cloud");
                    callback.onSuccess(items);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getLostItems failed: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Fetch ALL found items from Firestore (active + matched, not returned).
     */
    public void getFoundItems(ItemsCallback callback) {
        db.collection(COL_FOUND)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Item> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Item item = docToItem(doc, "Found");
                        if (!"Returned".equals(item.getStatus())) {
                            items.add(item);
                        }
                    }
                    Log.d(TAG, "Fetched " + items.size() + " found items from cloud");
                    callback.onSuccess(items);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getFoundItems failed: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    /** Convert an Item to a Firestore-compatible Map. */
    private Map<String, Object> buildMap(Item item) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId",         item.getUserId());
        map.put("itemName",       nvl(item.getItemName()));
        map.put("category",       nvl(item.getCategory()));
        map.put("description",    nvl(item.getDescription()));
        map.put("location",       nvl(item.getLocation()));
        map.put("date",           nvl(item.getDate()));
        map.put("status",         item.getStatus() != null ? item.getStatus() : "Active");
        map.put("itemType",       nvl(item.getItemType()));
        map.put("reporterName",   nvl(item.getReporterName()));
        map.put("reporterEmail",  nvl(item.getReporterEmail()));
        map.put("reporterRollNo", nvl(item.getReporterRollNo()));
        map.put("ownerCode",      nvl(item.getOwnerCode()));
        map.put("photoPath",      nvl(item.getPhotoPath()));
        map.put("timestamp",      com.google.firebase.firestore.FieldValue.serverTimestamp());
        return map;
    }

    /** Convert a Firestore document snapshot to an Item object. */
    private Item docToItem(QueryDocumentSnapshot doc, String type) {
        Item item = new Item();
        item.setFirestoreId(doc.getId());
        item.setItemType(type);

        Long userId = doc.getLong("userId");
        item.setUserId(userId != null ? userId.intValue() : 0);

        item.setItemName(doc.getString("itemName"));
        item.setCategory(doc.getString("category"));
        item.setDescription(doc.getString("description"));
        item.setLocation(doc.getString("location"));
        item.setDate(doc.getString("date"));
        item.setStatus(doc.getString("status"));
        item.setReporterName(doc.getString("reporterName"));
        item.setReporterEmail(doc.getString("reporterEmail"));
        item.setReporterRollNo(doc.getString("reporterRollNo"));
        item.setOwnerCode(doc.getString("ownerCode"));
        item.setPhotoPath(doc.getString("photoPath"));
        return item;
    }

    /** Null-safe empty string. */
    private String nvl(String s) {
        return s != null ? s : "";
    }
}
