package com.example.s3305.v001;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLdb extends SQLiteOpenHelper {
    //資料庫
    private final static String DB = "DB.db";
    //資料表
    private final static String TB = "TB";
    //版本
    private final static int VS = 2;



    public SQLdb(Context context) {
        //super(context, name, factory, version);
        super(context, DB, null, VS);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL = "CREATE TABLE IF NOT EXISTS "+TB+"(_date TEXT,_time TEXT,_Block TEXT,_temperature TEXT)";
        db.execSQL(SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String SQL = "DROP TABLE "+TB;
        db.execSQL(SQL);
    }
}