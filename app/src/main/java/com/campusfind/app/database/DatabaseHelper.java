package com.campusfind.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.campusfind.app.models.Claim;
import com.campusfind.app.models.Item;
import com.campusfind.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "CampusFind.db";
    private static final int DATABASE_VERSION = 2;

    // Table Names
    public static final String TABLE_USERS = "Users";
    public static final String TABLE_LOST_ITEMS = "LostItems";
    public static final String TABLE_FOUND_ITEMS = "FoundItems";
    public static final String TABLE_CLAIMS = "Claims";

    // Common Column Names
    public static final String COL_USER_ID = "user_id";
    public static final String COL_ITEM_ID = "item_id";
    public static final String COL_ITEM_NAME = "item_name";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_STATUS = "status";

    // Users Table Columns
    public static final String COL_USER_NAME = "name";
    public static final String COL_ROLL_NO = "roll_no";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";

    // LostItems Table Columns
    public static final String COL_LOCATION_LOST = "location_lost";
    public static final String COL_DATE_LOST = "date_lost";

    // FoundItems Table Columns
    public static final String COL_LOCATION_FOUND = "location_found";
    public static final String COL_DATE_FOUND = "date_found";

    // Claims Table Columns
    public static final String COL_CLAIM_ID = "claim_id";
    public static final String COL_ITEM_TYPE = "item_type";
    public static final String COL_QR_PAYLOAD = "qr_payload";
    public static final String COL_CLAIM_STATUS = "claim_status";
    public static final String COL_PHOTO_PATH = "photo_path";

    // Create Table Statements
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + " ("
            + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_USER_NAME + " TEXT NOT NULL, "
            + COL_ROLL_NO + " TEXT UNIQUE NOT NULL, "
            + COL_EMAIL + " TEXT UNIQUE NOT NULL, "
            + COL_PASSWORD + " TEXT NOT NULL"
            + ");";

    private static final String CREATE_TABLE_LOST_ITEMS = "CREATE TABLE " + TABLE_LOST_ITEMS + " ("
            + COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_USER_ID + " INTEGER, "
            + COL_ITEM_NAME + " TEXT NOT NULL, "
            + COL_CATEGORY + " TEXT NOT NULL, "
            + COL_DESCRIPTION + " TEXT, "
            + COL_LOCATION_LOST + " TEXT NOT NULL, "
            + COL_DATE_LOST + " TEXT NOT NULL, "
            + COL_STATUS + " TEXT DEFAULT 'Active', "
            + "FOREIGN KEY(" + COL_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ") ON DELETE CASCADE"
            + ");";

    private static final String CREATE_TABLE_FOUND_ITEMS = "CREATE TABLE " + TABLE_FOUND_ITEMS + " ("
            + COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_USER_ID + " INTEGER, "
            + COL_ITEM_NAME + " TEXT NOT NULL, "
            + COL_CATEGORY + " TEXT NOT NULL, "
            + COL_DESCRIPTION + " TEXT, "
            + COL_LOCATION_FOUND + " TEXT NOT NULL, "
            + COL_DATE_FOUND + " TEXT NOT NULL, "
            + COL_STATUS + " TEXT DEFAULT 'Active', "
            + COL_PHOTO_PATH + " TEXT, "
            + "FOREIGN KEY(" + COL_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ") ON DELETE CASCADE"
            + ");";

    private static final String CREATE_TABLE_CLAIMS = "CREATE TABLE " + TABLE_CLAIMS + " ("
            + COL_CLAIM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_USER_ID + " INTEGER, "
            + COL_ITEM_ID + " INTEGER NOT NULL, "
            + COL_ITEM_TYPE + " TEXT NOT NULL, "
            + COL_QR_PAYLOAD + " TEXT UNIQUE NOT NULL, "
            + COL_CLAIM_STATUS + " TEXT DEFAULT 'Pending', "
            + "FOREIGN KEY(" + COL_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ") ON DELETE CASCADE"
            + ");";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_LOST_ITEMS);
        db.execSQL(CREATE_TABLE_FOUND_ITEMS);
        db.execSQL(CREATE_TABLE_CLAIMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Migration: add photo_path column to FoundItems (preserves all existing data)
            try {
                db.execSQL("ALTER TABLE " + TABLE_FOUND_ITEMS + " ADD COLUMN " + COL_PHOTO_PATH + " TEXT");
            } catch (Exception e) {
                Log.w(TAG, "photo_path column already exists or migration failed", e);
            }
        }
        if (oldVersion < 1) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLAIMS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOUND_ITEMS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOST_ITEMS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }

    // =========================================================================
    // USER OPERATIONS
    // =========================================================================

    public long registerUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_NAME, user.getName().trim());
        values.put(COL_ROLL_NO, user.getRollNo().trim().toUpperCase());
        values.put(COL_EMAIL, user.getEmail().trim().toLowerCase());
        values.put(COL_PASSWORD, user.getPassword());
        return db.insert(TABLE_USERS, null, values);
    }

    public boolean isEmailTaken(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_EMAIL + " = ?", new String[]{email.trim().toLowerCase()},
                null, null, null);
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }

    public boolean isRollNoTaken(String rollNo) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_ROLL_NO + " = ?", new String[]{rollNo.trim().toUpperCase()},
                null, null, null);
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }

    public User authenticateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;
        Cursor cursor = db.query(TABLE_USERS, null,
                COL_EMAIL + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{email.trim().toLowerCase(), password},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public User getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;
        Cursor cursor = db.query(TABLE_USERS, null,
                COL_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID)));
        user.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)));
        user.setRollNo(cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLL_NO)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)));
        return user;
    }

    // =========================================================================
    // LOST ITEMS OPERATIONS
    // =========================================================================

    public long addLostItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_ID, item.getUserId());
        values.put(COL_ITEM_NAME, item.getItemName().trim());
        values.put(COL_CATEGORY, item.getCategory().trim());
        values.put(COL_DESCRIPTION, item.getDescription() != null ? item.getDescription().trim() : "");
        values.put(COL_LOCATION_LOST, item.getLocation().trim());
        values.put(COL_DATE_LOST, item.getDate().trim());
        values.put(COL_STATUS, item.getStatus() != null ? item.getStatus() : "Active");
        return db.insert(TABLE_LOST_ITEMS, null, values);
    }

    public List<Item> getLostItems(String query, String category) {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder sql = new StringBuilder("SELECT l.*, u." + COL_USER_NAME + ", u." + COL_ROLL_NO + ", u." + COL_EMAIL
                + " FROM " + TABLE_LOST_ITEMS + " l "
                + " LEFT JOIN " + TABLE_USERS + " u ON l." + COL_USER_ID + " = u." + COL_USER_ID
                + " WHERE 1=1 ");

        List<String> args = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (l." + COL_ITEM_NAME + " LIKE ? OR l." + COL_DESCRIPTION + " LIKE ? OR l." + COL_LOCATION_LOST + " LIKE ?)");
            String param = "%" + query.trim() + "%";
            args.add(param);
            args.add(param);
            args.add(param);
        }
        if (category != null && !category.equalsIgnoreCase("All Categories") && !category.trim().isEmpty()) {
            sql.append(" AND l." + COL_CATEGORY + " = ?");
            args.add(category.trim());
        }
        sql.append(" ORDER BY l." + COL_ITEM_ID + " DESC");

        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Item item = cursorToLostItem(cursor);
                list.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<Item> getLostItemsByUser(int userId) {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT l.*, u." + COL_USER_NAME + ", u." + COL_ROLL_NO + ", u." + COL_EMAIL
                + " FROM " + TABLE_LOST_ITEMS + " l "
                + " LEFT JOIN " + TABLE_USERS + " u ON l." + COL_USER_ID + " = u." + COL_USER_ID
                + " WHERE l." + COL_USER_ID + " = ? "
                + " ORDER BY l." + COL_ITEM_ID + " DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToLostItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public Item getLostItemById(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT l.*, u." + COL_USER_NAME + ", u." + COL_ROLL_NO + ", u." + COL_EMAIL
                + " FROM " + TABLE_LOST_ITEMS + " l "
                + " LEFT JOIN " + TABLE_USERS + " u ON l." + COL_USER_ID + " = u." + COL_USER_ID
                + " WHERE l." + COL_ITEM_ID + " = ?";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(itemId)});
        Item item = null;
        if (cursor != null && cursor.moveToFirst()) {
            item = cursorToLostItem(cursor);
            cursor.close();
        }
        return item;
    }

    private Item cursorToLostItem(Cursor cursor) {
        Item item = new Item();
        item.setItemId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_ID)));
        item.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID)));
        item.setItemType("Lost");
        item.setItemName(cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_NAME)));
        item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
        item.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
        item.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION_LOST)));
        item.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE_LOST)));
        item.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)));
        item.setReporterName(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)));
        item.setReporterRollNo(cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLL_NO)));
        item.setReporterEmail(cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)));
        return item;
    }

    // =========================================================================
    // FOUND ITEMS OPERATIONS
    // =========================================================================

    public long addFoundItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_ID, item.getUserId());
        values.put(COL_ITEM_NAME, item.getItemName().trim());
        values.put(COL_CATEGORY, item.getCategory().trim());
        values.put(COL_DESCRIPTION, item.getDescription() != null ? item.getDescription().trim() : "");
        values.put(COL_LOCATION_FOUND, item.getLocation().trim());
        values.put(COL_DATE_FOUND, item.getDate().trim());
        values.put(COL_STATUS, item.getStatus() != null ? item.getStatus() : "Active");
        values.put(COL_PHOTO_PATH, item.getPhotoPath() != null ? item.getPhotoPath() : "");
        return db.insert(TABLE_FOUND_ITEMS, null, values);
    }

    public List<Item> getFoundItems(String query, String category) {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder sql = new StringBuilder("SELECT f.*, u." + COL_USER_NAME + ", u." + COL_ROLL_NO + ", u." + COL_EMAIL
                + " FROM " + TABLE_FOUND_ITEMS + " f "
                + " LEFT JOIN " + TABLE_USERS + " u ON f." + COL_USER_ID + " = u." + COL_USER_ID
                + " WHERE 1=1 ");

        List<String> args = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (f." + COL_ITEM_NAME + " LIKE ? OR f." + COL_DESCRIPTION + " LIKE ? OR f." + COL_LOCATION_FOUND + " LIKE ?)");
            String param = "%" + query.trim() + "%";
            args.add(param);
            args.add(param);
            args.add(param);
        }
        if (category != null && !category.equalsIgnoreCase("All Categories") && !category.trim().isEmpty()) {
            sql.append(" AND f." + COL_CATEGORY + " = ?");
            args.add(category.trim());
        }
        sql.append(" ORDER BY f." + COL_ITEM_ID + " DESC");

        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToFoundItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<Item> getFoundItemsByUser(int userId) {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT f.*, u." + COL_USER_NAME + ", u." + COL_ROLL_NO + ", u." + COL_EMAIL
                + " FROM " + TABLE_FOUND_ITEMS + " f "
                + " LEFT JOIN " + TABLE_USERS + " u ON f." + COL_USER_ID + " = u." + COL_USER_ID
                + " WHERE f." + COL_USER_ID + " = ? "
                + " ORDER BY f." + COL_ITEM_ID + " DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToFoundItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public Item getFoundItemById(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT f.*, u." + COL_USER_NAME + ", u." + COL_ROLL_NO + ", u." + COL_EMAIL
                + " FROM " + TABLE_FOUND_ITEMS + " f "
                + " LEFT JOIN " + TABLE_USERS + " u ON f." + COL_USER_ID + " = u." + COL_USER_ID
                + " WHERE f." + COL_ITEM_ID + " = ?";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(itemId)});
        Item item = null;
        if (cursor != null && cursor.moveToFirst()) {
            item = cursorToFoundItem(cursor);
            cursor.close();
        }
        return item;
    }

    private Item cursorToFoundItem(Cursor cursor) {
        Item item = new Item();
        item.setItemId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_ID)));
        item.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID)));
        item.setItemType("Found");
        item.setItemName(cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_NAME)));
        item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
        item.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
        item.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION_FOUND)));
        item.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE_FOUND)));
        item.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)));
        int photoCol = cursor.getColumnIndex(COL_PHOTO_PATH);
        if (photoCol != -1) item.setPhotoPath(cursor.getString(photoCol));
        item.setReporterName(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)));
        item.setReporterRollNo(cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLL_NO)));
        item.setReporterEmail(cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)));
        return item;
    }

    public boolean updateItemStatus(int itemId, String itemType, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);
        String table = itemType.equalsIgnoreCase("Lost") ? TABLE_LOST_ITEMS : TABLE_FOUND_ITEMS;
        int rows = db.update(table, values, COL_ITEM_ID + " = ?", new String[]{String.valueOf(itemId)});
        return rows > 0;
    }

    // =========================================================================
    // CLAIMS OPERATIONS
    // =========================================================================

    public long addClaim(Claim claim) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_ID, claim.getUserId());
        values.put(COL_ITEM_ID, claim.getItemId());
        values.put(COL_ITEM_TYPE, claim.getItemType());
        values.put(COL_QR_PAYLOAD, claim.getQrPayload());
        values.put(COL_CLAIM_STATUS, claim.getClaimStatus() != null ? claim.getClaimStatus() : "Pending");
        return db.insert(TABLE_CLAIMS, null, values);
    }

    public Claim getClaimForUserAndItem(int userId, int itemId, String itemType) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CLAIMS, null,
                COL_USER_ID + " = ? AND " + COL_ITEM_ID + " = ? AND " + COL_ITEM_TYPE + " = ?",
                new String[]{String.valueOf(userId), String.valueOf(itemId), itemType},
                null, null, null);

        Claim claim = null;
        if (cursor != null && cursor.moveToFirst()) {
            claim = cursorToClaim(cursor);
            cursor.close();
        }
        return claim;
    }

    public List<Claim> getClaimsByUser(int userId) {
        List<Claim> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT c.*, "
                + "CASE WHEN c." + COL_ITEM_TYPE + " = 'Lost' THEN l." + COL_ITEM_NAME + " ELSE f." + COL_ITEM_NAME + " END AS item_name "
                + "FROM " + TABLE_CLAIMS + " c "
                + "LEFT JOIN " + TABLE_LOST_ITEMS + " l ON c." + COL_ITEM_ID + " = l." + COL_ITEM_ID + " AND c." + COL_ITEM_TYPE + " = 'Lost' "
                + "LEFT JOIN " + TABLE_FOUND_ITEMS + " f ON c." + COL_ITEM_ID + " = f." + COL_ITEM_ID + " AND c." + COL_ITEM_TYPE + " = 'Found' "
                + "WHERE c." + COL_USER_ID + " = ? "
                + "ORDER BY c." + COL_CLAIM_ID + " DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Claim claim = cursorToClaim(cursor);
                try {
                    int colName = cursor.getColumnIndex("item_name");
                    if (colName != -1) {
                        claim.setItemName(cursor.getString(colName));
                    }
                } catch (Exception ignored) {}
                list.add(claim);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public Claim getClaimByQrPayload(String qrPayload) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT c.*, u." + COL_USER_NAME + ", u." + COL_ROLL_NO + ", u." + COL_EMAIL + ", "
                + "CASE WHEN c." + COL_ITEM_TYPE + " = 'Lost' THEN l." + COL_ITEM_NAME + " ELSE f." + COL_ITEM_NAME + " END AS item_name "
                + "FROM " + TABLE_CLAIMS + " c "
                + "LEFT JOIN " + TABLE_USERS + " u ON c." + COL_USER_ID + " = u." + COL_USER_ID + " "
                + "LEFT JOIN " + TABLE_LOST_ITEMS + " l ON c." + COL_ITEM_ID + " = l." + COL_ITEM_ID + " AND c." + COL_ITEM_TYPE + " = 'Lost' "
                + "LEFT JOIN " + TABLE_FOUND_ITEMS + " f ON c." + COL_ITEM_ID + " = f." + COL_ITEM_ID + " AND c." + COL_ITEM_TYPE + " = 'Found' "
                + "WHERE c." + COL_QR_PAYLOAD + " = ?";

        Cursor cursor = db.rawQuery(sql, new String[]{qrPayload.trim()});
        Claim claim = null;
        if (cursor != null && cursor.moveToFirst()) {
            claim = cursorToClaim(cursor);
            claim.setClaimerName(cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)));
            claim.setClaimerRollNo(cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLL_NO)));
            claim.setClaimerEmail(cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)));
            int colName = cursor.getColumnIndex("item_name");
            if (colName != -1) {
                claim.setItemName(cursor.getString(colName));
            }
            cursor.close();
        }
        return claim;
    }

    public boolean updateClaimStatus(int claimId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CLAIM_STATUS, status);
        int rows = db.update(TABLE_CLAIMS, values, COL_CLAIM_ID + " = ?", new String[]{String.valueOf(claimId)});
        return rows > 0;
    }

    /**
     * Executes the handover verification in an atomic SQLite transaction:
     * 1. Finds claim by qr_payload
     * 2. Updates claim_status -> 'Returned'
     * 3. Updates corresponding item status in LostItems / FoundItems -> 'Returned'
     */
    public boolean verifyAndCompleteHandover(String qrPayload) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            Claim claim = getClaimByQrPayload(qrPayload);
            if (claim == null) {
                return false;
            }

            // Update Claim status to 'Returned'
            ContentValues claimValues = new ContentValues();
            claimValues.put(COL_CLAIM_STATUS, "Returned");
            int claimRows = db.update(TABLE_CLAIMS, claimValues,
                    COL_CLAIM_ID + " = ?", new String[]{String.valueOf(claim.getClaimId())});

            // Update corresponding item status to 'Returned'
            String table = claim.getItemType().equalsIgnoreCase("Lost") ? TABLE_LOST_ITEMS : TABLE_FOUND_ITEMS;
            ContentValues itemValues = new ContentValues();
            itemValues.put(COL_STATUS, "Returned");
            int itemRows = db.update(table, itemValues,
                    COL_ITEM_ID + " = ?", new String[]{String.valueOf(claim.getItemId())});

            if (claimRows > 0 && itemRows > 0) {
                db.setTransactionSuccessful();
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error during handover transaction", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    private Claim cursorToClaim(Cursor cursor) {
        Claim claim = new Claim();
        claim.setClaimId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_CLAIM_ID)));
        claim.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID)));
        claim.setItemId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_ID)));
        claim.setItemType(cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_TYPE)));
        claim.setQrPayload(cursor.getString(cursor.getColumnIndexOrThrow(COL_QR_PAYLOAD)));
        claim.setClaimStatus(cursor.getString(cursor.getColumnIndexOrThrow(COL_CLAIM_STATUS)));
        return claim;
    }
}
