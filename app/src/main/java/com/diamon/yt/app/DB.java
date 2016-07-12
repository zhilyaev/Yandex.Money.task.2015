package com.diamon.yt.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DB extends SQLiteOpenHelper {

    public DB(Context context) {
        // конструктор суперкласса
        super(context, "TY_DB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Посроим стандартный sql-запрос для создания таблицы
        db.execSQL("create table Cats ("
                + "id integer primary key autoincrement,"
                + "id integer,"
                + "parent_id integer,"
                + "title text" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
