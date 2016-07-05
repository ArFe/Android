package br.com.ideiageni.uct;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by ariel on 13/05/2016.
 */
public class ModbusDBHelper extends SQLiteOpenHelper {

    final static String TABLE_NAME = "ModbusHR";
    final static String _ID = "_id";
    final static String VALUE = "value";
    final static String[] columns = new String[] {_ID,VALUE};

    final private static String CREATE_CMD =

            "CREATE TABLE " + TABLE_NAME
                    + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", " + VALUE + " INTEGER"
                    + ")";

    final private static String NAME = "modbus_db";
    final private static Integer VERSION = 1;
    final private Context mContext;

    public ModbusDBHelper(Context context) {
        super(context, NAME, null, VERSION);
        this.mContext = context;

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CMD);
        Log.d("UCT","Database Created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // N/A
    }

    void deleteDatabase() {
        mContext.deleteDatabase(NAME);
    }

}
