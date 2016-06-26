/*
 DataManager.java
 Copyright (c) 2016 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.app.simplebot.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * データ管理クラス
 */
public class DataManager {

    /** デバッグタグ */
    private static final String TAG = "DataManager";
    /** デバッグフラグ */
    private static final boolean DEBUG = true;

    /** DBのバージョン */
    private static final int DB_VERSION = 1;
    /** DBファイル名 */
    public static final String DB_NAME = "simplebot.db";
    /** テーブル名 */
    public static final String TABLE_NAME = "command";

    /** idのカラム名 */
    public static final String COLUMN_ID = "_id";
    /** keywordのカラム名 */
    public static final String COLUMN_KEYWORD = "keyword";
    /** serviceIdのカラム名 */
    public static final String COLUMN_SERVICE_ID = "serviceId";
    /** serviceNameのカラム名 */
    public static final String COLUMN_SERVICE_NAME = "serviceName";
    /** methodのカラム名 */
    public static final String COLUMN_METHOD = "method";
    /** pathのカラム名 */
    public static final String COLUMN_PATH = "path";
    /** bodyのカラム名 */
    public static final String COLUMN_BODY = "body";
    /** successのカラム名 */
    public static final String COLUMN_SUCCESS = "success";
    /** errorのカラム名 */
    public static final String COLUMN_ERROR = "error";

    /** 全カラム */
    public static final String[] COLUMNS = {
            COLUMN_ID,
            COLUMN_KEYWORD, // キーワード
            COLUMN_SERVICE_ID, // サービスID
            COLUMN_SERVICE_NAME, // サービス名
            COLUMN_METHOD, // メソッド
            COLUMN_PATH, // パス
            COLUMN_BODY, // ボディ
            COLUMN_SUCCESS, // 成功レスポンス
            COLUMN_ERROR // 失敗レスポンス
    };

    /** SQLiteHelper */
    private SQLiteHelper sql;

    /**
     * データクラス
     */
    public static class Data {
        public long id = -1;
        public String keyword;
        public String serviceId;
        public String serviceName;
        public String method;
        public String path;
        public String body;
        public String success;
        public String error;

        @Override
        public String toString() {
            return "Data{" +
                    "id=" + id +
                    ", keyword='" + keyword + '\'' +
                    ", serviceId='" + serviceId + '\'' +
                    ", serviceName='" + serviceName + '\'' +
                    ", method='" + method + '\'' +
                    ", path='" + path + '\'' +
                    ", body='" + body + '\'' +
                    ", success='" + success + '\'' +
                    ", error='" + error + '\'' +
                    '}';
        }
    }

    /**
     * SQLHelper
     */
    private class SQLiteHelper extends SQLiteOpenHelper {

        /**
         * context指定で初期化
         * @param context コンテキスト
         */
        public SQLiteHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            StringBuilder sb = new StringBuilder();
            sb.append("create table ");
            sb.append(TABLE_NAME);
            sb.append(" (");
            sb.append(COLUMN_ID);
            sb.append(" integer PRIMARY KEY AUTOINCREMENT");
            for (String col: COLUMNS) {
                if (col.equals(COLUMN_ID)) {
                    continue;
                } else {
                    sb.append(", ");
                }
                sb.append(col);
                sb.append(" text");
            }
            sb.append(");");
            if (DEBUG) Log.d(TAG,"onCreate:" + sb.toString());
            db.execSQL(sb.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (DEBUG) Log.d(TAG,"onUpgrade");
            throw new RuntimeException("Database Version is Invalid");
        }
    }

    /**
     * context指定で初期化
     * @param context コンテキスト
     */
    public DataManager(Context context) {
        sql = new SQLiteHelper(context);
    }

    /**
     * データを登録
     * @param data データ
     * @return trueで成功
     */
    public boolean upsert(Data data) {
        if (DEBUG) Log.d(TAG,"upsert:" + data.toString());
        SQLiteDatabase db = sql.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_KEYWORD, data.keyword);
        values.put(COLUMN_SERVICE_ID, data.serviceId);
        values.put(COLUMN_SERVICE_NAME, data.serviceName);
        values.put(COLUMN_METHOD, data.method);
        values.put(COLUMN_PATH, data.path);
        values.put(COLUMN_BODY, data.body);
        values.put(COLUMN_SUCCESS, data.success);
        values.put(COLUMN_ERROR, data.error);

        long ret;
        if (data.id < 0) {
            ret = db.insert(TABLE_NAME, null, values);
        } else {
            ret = db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(data.id)});
        }
        return ret > 0;
    }

    /**
     * データを削除
     * @param id ID
     * @return trueで成功
     */
    public boolean delete(long id) {
        if (DEBUG) Log.d(TAG,"upsert:" + String.valueOf(id));
        SQLiteDatabase db = sql.getWritableDatabase();
        long ret = db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return ret > 0;
    }

    /**
     * 全てのデータを返すカーソル取得
     * @return カーソル
     */
    public Cursor getAll() {
        if (DEBUG) Log.d(TAG,"getAll");
        SQLiteDatabase db = sql.getReadableDatabase();
        return db.query(TABLE_NAME, COLUMNS, null, null, null, null, COLUMN_ID + " desc");
    }

    /**
     * ID指定でデータを取得
     * @param id ID
     * @return データ
     */
    public Data getData(long id) {
        if (DEBUG) Log.d(TAG,"getData:" + id);
        SQLiteDatabase db = sql.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, COLUMNS, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, COLUMN_ID + " desc");
        if (cursor.moveToFirst()) {
            Data data = convertData(cursor);
            cursor.close();
            return data;
        } else {
            cursor.close();
            return null;
        }
    }

    /**
     * SQLiteのカーソルから情報を取得
     * @param cursor カーソル
     * @return データ
     */
    public Data convertData(Cursor cursor) {
        Data data = new Data();
        data.id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
        data.keyword = cursor.getString(cursor.getColumnIndex(COLUMN_KEYWORD));
        data.serviceId = cursor.getString(cursor.getColumnIndex(COLUMN_SERVICE_ID));
        data.serviceName = cursor.getString(cursor.getColumnIndex(COLUMN_SERVICE_NAME));
        data.method = cursor.getString(cursor.getColumnIndex(COLUMN_METHOD));
        data.path = cursor.getString(cursor.getColumnIndex(COLUMN_PATH));
        data.body = cursor.getString(cursor.getColumnIndex(COLUMN_BODY));
        data.success = cursor.getString(cursor.getColumnIndex(COLUMN_SUCCESS));
        data.error = cursor.getString(cursor.getColumnIndex(COLUMN_ERROR));
        return data;
    }
}
