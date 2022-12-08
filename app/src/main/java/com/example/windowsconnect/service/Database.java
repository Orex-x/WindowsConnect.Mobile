package com.example.windowsconnect.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.windowsconnect.models.Host;

import java.util.ArrayList;

public class Database extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "WindowsConnect";
    public static final String COLUMN_ID = "id";

    public static final String TABLE_HOST = "Host";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_IP = "ip";
    public static final String COLUMN_MAC_ADDRESS = "macAddress";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_HOST + " ("
                + COLUMN_ID + " integer primary key autoincrement,"
                + COLUMN_NAME + " text,"
                + COLUMN_IP + " text,"
                + COLUMN_MAC_ADDRESS + " text" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HOST);
        onCreate(db);
    }

    public boolean insertHost(Host host) {
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_NAME, host.getName());
            contentValues.put(COLUMN_IP, host.getLocalIP());
            contentValues.put(COLUMN_MAC_ADDRESS, host.getMacAddress());
            db.insert(TABLE_HOST, null, contentValues);
            return true;
        }catch (Exception e){

        }
        return false;
    }

        public ArrayList<Host> getAllHosts() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HOST,
                null, null, null, null, null, null);
        ArrayList<Host> users = new ArrayList<>();
        Host host;
        if (cursor.getCount() > 0) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                host = new Host();
                host.setName(cursor.getString(1));
                host.setLocalIP(cursor.getString(2));
                host.setMacAddress(cursor.getString(3));
                users.add(host);
            }
        }
        cursor.close();
        db.close();
        return users;
    }

    public boolean deleteHost(String name){
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DELETE FROM " + TABLE_HOST + " WHERE " + COLUMN_NAME + " = '" + name + "';");
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

}
