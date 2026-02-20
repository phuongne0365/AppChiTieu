package com.example.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SmartWallet.db";
    private static final int DATABASE_VERSION = 5;

    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_IS_EXPENSE = "is_expense";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_TIME + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_IS_EXPENSE + " INTEGER);");
        
        db.execSQL("CREATE TABLE IF NOT EXISTS users (" +
                "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE, " +
                "password TEXT, " +
                "fullname TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COLUMN_TIMESTAMP + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 5) {
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + ", " + COLUMN_TIME + " FROM " + TABLE_TRANSACTIONS, null);
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String timeStr = cursor.getString(1);
                    long ts = parseTimeToTimestamp(timeStr);
                    if (ts > 0) {
                        db.execSQL("UPDATE " + TABLE_TRANSACTIONS + " SET " + COLUMN_TIMESTAMP + " = " + ts + " WHERE " + COLUMN_ID + " = " + id);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    private long parseTimeToTimestamp(String timeStr) {
        try {
            String[] parts = timeStr.split("-");
            if (parts.length < 2) return 0;
            String datePart = parts[1].trim(); 
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            int year = datePart.endsWith("/10") || datePart.endsWith("/11") || datePart.endsWith("/12") ? 2025 : 2026;
            Date date = sdf.parse(datePart + "/" + year);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // --- User Methods ---
    public boolean registerUser(String username, String password, String fullname) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        values.put("fullname", fullname);
        long result = db.insert("users", null, values);
        db.close();
        return result != -1;
    }

    public String checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT fullname FROM users WHERE username = ? AND password = ?", new String[]{username, password});
        String fullname = null;
        if (cursor.moveToFirst()) {
            fullname = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return fullname;
    }

    // --- Transaction Methods ---
    public long addTransaction(String title, double amount, String time, String category, boolean isExpense) {
        return addTransaction(title, amount, time, category, isExpense, System.currentTimeMillis());
    }

    // Mới: Hỗ trợ truyền timestamp chính xác (có cả năm)
    public long addTransaction(String title, double amount, String time, String category, boolean isExpense, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_TIME, time);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_IS_EXPENSE, isExpense ? 1 : 0);
        long id = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return id;
    }

    public void deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<TransactionAdapter.Transaction> getFilteredTransactions(long start, long end) {
        List<TransactionAdapter.Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + 
                       " WHERE " + COLUMN_TIMESTAMP + " >= ? AND " + COLUMN_TIMESTAMP + " <= ?" +
                       " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)});
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));
                boolean isExpense = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_EXPENSE)) == 1;
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                transactions.add(new TransactionAdapter.Transaction(id, title, time, amount, isExpense, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }

    public List<TransactionAdapter.Transaction> getAllTransactions() {
        return getFilteredTransactions(0, Long.MAX_VALUE);
    }

    public double getTotalIncome(long start, long end) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + 
                " WHERE " + COLUMN_IS_EXPENSE + " = 0 AND " + COLUMN_TIMESTAMP + " BETWEEN ? AND ?", 
                new String[]{String.valueOf(start), String.valueOf(end)});
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    public double getTotalExpense(long start, long end) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + 
                " WHERE " + COLUMN_IS_EXPENSE + " = 1 AND " + COLUMN_TIMESTAMP + " BETWEEN ? AND ?", 
                new String[]{String.valueOf(start), String.valueOf(end)});
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    public Map<String, Double> getSpendingStats(long start, long end) {
        Map<String, Double> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") as total " +
                       "FROM " + TABLE_TRANSACTIONS + " " +
                       "WHERE " + COLUMN_IS_EXPENSE + " = 1 AND " + COLUMN_TIMESTAMP + " BETWEEN ? AND ?" +
                       " GROUP BY " + COLUMN_CATEGORY;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)});
        if (cursor.moveToFirst()) {
            do {
                stats.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return stats;
    }

    public Map<String, Double> getIncomeStats(long start, long end) {
        Map<String, Double> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") as total " +
                       "FROM " + TABLE_TRANSACTIONS + " " +
                       "WHERE " + COLUMN_IS_EXPENSE + " = 0 AND " + COLUMN_TIMESTAMP + " BETWEEN ? AND ?" +
                       " GROUP BY " + COLUMN_CATEGORY;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)});
        if (cursor.moveToFirst()) {
            do {
                stats.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return stats;
    }
}