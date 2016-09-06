package br.com.ideiageni.mama;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Date;

public class FeedingActivity extends AppCompatActivity {


    private DatabaseOpenHelper mDbHelper;
    private SimpleCursorAdapter mAdapter;
    Cursor c;

    private RadioGroup radioGroup;
    private RadioButton leftRadioButton;
    private RadioButton rightRadioButton;

    public String status = "stopped";

    TextView leftTextView;
    TextView rightTextView;
    Spinner sideTextView;
    EditText startDateTextView;
    EditText finishDateTextView;
    EditText _timeTextView;
    int dbSide = 0;
    String dbStartDate;
    String dbFinishDate;
    long startTime = 0;
    long elapsedTime = 0;
    long currentTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feeding);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        leftRadioButton = (RadioButton) findViewById(R.id.leftRadioButton);
        rightRadioButton = (RadioButton) findViewById(R.id.rightRadioButton);
        leftTextView = (TextView) findViewById(R.id.leftTimeTV);
        rightTextView = (TextView) findViewById(R.id.rightTimeTV);
        sideTextView = (Spinner) findViewById(R.id.side);
        startDateTextView = (EditText) findViewById(R.id.startDate);
        finishDateTextView = (EditText) findViewById(R.id.finishDate);
        _timeTextView = (EditText) findViewById(R.id._time);

        sideTextView.setSelection(dbSide);


        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final FloatingActionButton fab1 = (FloatingActionButton) findViewById(R.id.fab1);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FloatingActionButton fab = (FloatingActionButton) view;
                fab1.setVisibility(View.VISIBLE);
                if (status == "started") {
                    timerHandler.removeCallbacks(timerRunnable);
                    fab.setImageResource(R.drawable.ic_media_play);
                    elapsedTime = System.currentTimeMillis() - startTime;
                    status = "paused";
                } else if (status == "stopped"){
                    startTime = System.currentTimeMillis();
                    dbStartDate = (String) DateFormat.format("dd-MM-yyy hh:mm", new Date());
                    timerHandler.postDelayed(timerRunnable, 0);
                    fab.setImageResource(R.drawable.ic_media_pause);
                    status = "started";
                } else if (status == "paused"){
                    startTime = System.currentTimeMillis() - elapsedTime;
                    timerHandler.postDelayed(timerRunnable, 0);
                    fab.setImageResource(R.drawable.ic_media_pause);
                    status = "started";
                }
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FloatingActionButton fab1 = (FloatingActionButton) view;
                fab1.setVisibility(View.INVISIBLE);
                fab.setImageResource(R.drawable.ic_media_play);
                timerHandler.removeCallbacks(timerRunnable);
                dbFinishDate = (String) DateFormat.format("dd-MM-yyy hh:mm", new Date());

                add2db();
                status = "stopped";
                // execute database operations
                //fix();

                // Redisplay data
                //c = readRecords();
                mAdapter.getCursor().requery();
                mAdapter.notifyDataSetChanged();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create a new DatabaseHelper
        mDbHelper = new DatabaseOpenHelper(this);

        // start with an empty database
        //clearAll();

        // Create a cursor
        c = readRecords();
        mAdapter = new SimpleCursorAdapter(this, R.layout.list_layout, c,
                DatabaseOpenHelper.columns, new int[] {R.id.lv_id, R.id.lv_side, R.id.lv_startDate, R.id.lv_finishDate, R.id.lv_time},
                0);
        ListView list = (ListView) findViewById(R.id.ListView);
        list.setAdapter(mAdapter);

    }



    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            currentTime = System.currentTimeMillis() - startTime;
            long millis = currentTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            String time = String.format("%d:%02d", minutes, seconds);

            if (leftRadioButton.isChecked()){
                leftTextView.setText(time);
                rightTextView.setText(R.string.timeText);
            } else {
                rightTextView.setText(time);
                leftTextView.setText(R.string.timeText);
            }

            if (leftRadioButton.isChecked()){
                dbSide = 0;
            } else {
                dbSide = 1;
            }

            sideTextView.setSelection(dbSide);
            startDateTextView.setText(dbStartDate);
            finishDateTextView.setText(dbFinishDate);
            _timeTextView.setText(String.format("%d", minutes));

            timerHandler.postDelayed(this, 500);
        }
    };

    // Insert several artist records
    private void add2db() {

        ContentValues values = new ContentValues();

        values.put(DatabaseOpenHelper.SIDE, dbSide);
        values.put(DatabaseOpenHelper.START_DATE, dbStartDate);
        values.put(DatabaseOpenHelper.FINISH_DATE, dbFinishDate);
        values.put(DatabaseOpenHelper.TIME, currentTime/1000);
        mDbHelper.getWritableDatabase().insert(DatabaseOpenHelper.TABLE_NAME, null, values);

        values.clear();

    }

    // Returns all artist records in the database
    private Cursor readRecords() {
        return mDbHelper.getWritableDatabase().query(DatabaseOpenHelper.TABLE_NAME,
                DatabaseOpenHelper.columns, null, new String[] {}, null, null,DatabaseOpenHelper._ID+" DESC");
    }


    // Delete all records
    private void clearAll() {

        mDbHelper.getWritableDatabase().delete(DatabaseOpenHelper.TABLE_NAME, null, null);

    }

    // Close database
    @Override
    protected void onDestroy() {

        mDbHelper.getWritableDatabase().close();
        //mDbHelper.deleteDatabase();

        super.onDestroy();

    }

}
