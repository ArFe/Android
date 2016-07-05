package br.com.ideiageni.uct;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by ariel on 24/11/2015.
 */
public class ModbusDataBase {
    public ModbusDBHelper mDbHelper;
    int dbSize = 16 * 48;
    int temp;


    public ModbusDataBase(Context context, int intDbsize){
        mDbHelper = new ModbusDBHelper(context);
        // start with an empty database
        //clearAll();
        mDbHelper.deleteDatabase();
        dbSize = intDbsize;
        dbCreate();

    }

    // Write one value at a time
    public void dbUpdate(int _id, int valor) {

        ContentValues values = new ContentValues();
        String strFilter = "_id=" + _id;
        values.put(ModbusDBHelper.VALUE, valor);
        mDbHelper.getWritableDatabase().update(ModbusDBHelper.TABLE_NAME, values, strFilter, null);
        values.clear();

    }

    // Write several values at once
    public void dbUpdate(int start_id, int[] valor) {


        ContentValues values = new ContentValues();
        for(int i=0;i<valor.length;i++) {
            int current_id = start_id + i;
            String strFilter = "_id=" + current_id;
            values.put(ModbusDBHelper.VALUE, valor[i]);
            mDbHelper.getWritableDatabase().update(ModbusDBHelper.TABLE_NAME, values, strFilter, null);
            values.clear();
        }

    }

    //
    public void dbCreate() {

        ContentValues values = new ContentValues();
        for(int i=0;i<dbSize;i++){

            values.put(ModbusDBHelper.VALUE,i);
            mDbHelper.getWritableDatabase().insert(ModbusDBHelper.TABLE_NAME, null, values);
            values.clear();
        }
    }

    public Cursor readRecords() {
        return mDbHelper.getWritableDatabase().query(ModbusDBHelper.TABLE_NAME,
                ModbusDBHelper.columns, null, new String[] {}, null, null,ModbusDBHelper._ID);
    }

    // Delete all records
    public void clearAll() {

        mDbHelper.getWritableDatabase().delete(ModbusDBHelper.TABLE_NAME, null, null);

    }

    // Close database
    public void dbClose() {

        mDbHelper.getWritableDatabase().close();
        mDbHelper.deleteDatabase();

    }

    public int[] getRegs(int id, int numRegs){

        // 1. get reference to readable DB
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // 2. build query
        Cursor cursor =
                db.query(ModbusDBHelper.TABLE_NAME,
                        ModbusDBHelper.columns,
                        " "+ ModbusDBHelper._ID + " = ?", // c. selections
                        new String[] { String.valueOf(id) }, // d. selections args
                        null, // e. group by
                        null, // f. having
                        null, // g. order by
                        null); // h. limit

        // 3. if we got results get the first one
        int[] data = new int[numRegs];
        temp = cursor.getCount();
        if (temp> 0) {
            cursor.moveToFirst();
            // 4. build book object
            for (int i = 0; i < numRegs; i++){
                data[i] = cursor.getInt(cursor.getColumnIndexOrThrow(ModbusDBHelper.VALUE));
                cursor.moveToNext();
            }
            //log
//        Log.d("getBook("+id+")", book.toString());
        }
        return data;
    }

}
