package com.example.s3305.v001;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.os.SystemClock;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LongRunningService extends Service {

    public SQLdb HD = null;

    String Time,Block,S_Date;
    int count = 0;
    Double Temperature;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df_date = new SimpleDateFormat("yyyy/MM/dd");
                S_Date = df_date.format(c.getTime());
                SimpleDateFormat df_time = new SimpleDateFormat("HH:mm:ss");
                Time = df_time.format(c.getTime());
                Block = "1";
                Temperature = 25.2;
                count++;
                AddToSQL();
                if (count == 120) {
                    count = 0;
                    AddToFb();
                }
            }
        }).start();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 30 * 1000; // 設定巡迴時間
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        manager.set(AlarmManager.RTC_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }


    private void AddToSQL() {
        HD = new SQLdb(this);
        SQLiteDatabase db = HD.getWritableDatabase();
        ContentValues values_T = new ContentValues();
        ContentValues values_B = new ContentValues();
        ContentValues values_D = new ContentValues();
        ContentValues values_t = new ContentValues();

        //載入資料
        values_t.put("_temperature", Temperature);
        values_B.put("_Block", Block);
        values_D.put("_Date", S_Date);
        values_T.put("_Time", Time);

        //寫入資料庫
        db.insert("tb", null, values_T);
        db.insert("tb", null, values_D);
        db.insert("tb", null, values_B);
        db.insert("tb", null, values_t);
    }

    private void AddToFb() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        HD = new SQLdb(this);
        SQLiteDatabase db = HD.getWritableDatabase();
        //查詢資料庫
        Cursor cursor = db.query("TB", new String[]{"_Date", "_Time", "_Block", "_temperature"}, null, null, null, null, null);
        cursor.moveToFirst();

        String time = null;
        String block = null;
        String date = null;
        for (int i = 0; i < cursor.getCount(); i++) {
            if ((i % 4) == 0) {
                time = cursor.getString(1);
            } else if ((i % 4) == 1) {
                date = cursor.getString(0);
            } else if ((i % 4) == 2) {
                block = cursor.getString(2);
            } else {
                DatabaseReference myRef = database.getReference(date + "/" + block + "/" + time);
                myRef.setValue(cursor.getDouble(3));
            }
            cursor.moveToNext();
        }
        db.delete("tb",null,null);
    }
}

