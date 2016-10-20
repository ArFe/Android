package br.com.ideiageni.uct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

public class ReadRegActivity extends AppCompatActivity {

    CommClass comm;

    int numReg = 16;
    int numNodes = 15;
    byte node = 0;
    boolean[] nodes = new boolean[47];
    int readNodes = 0;
    boolean autoRead = false;
    int autoReadTime = 2000;
    int timeOutTime = 2000;

    byte mSlave = 1;
    byte mCmd = 3;
    byte mAddrHi = 0;
    byte mAddrLo = 16;
    final byte mLenHi = 0;
    final byte mLenLo = (byte) numReg;
    int expectedBytes = 37;
    Modbus slave1 = new Modbus(mSlave);
    ReadThread rt;


    private SimpleCursorAdapter mAdapter;
    private ModbusDataBase mDataBase;
    Cursor c;
    ListView list;

    EditText etAutoRead;
    RadioButton rbAuto;
    RadioButton rbManual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_reg);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        comm = new CommClass(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);
        this.registerReceiver(mUsbReceiver, filter);

        ArrayList<String> nodeNums = new ArrayList<>();
        for(int i=0;i<numNodes;i++)nodeNums.add(String.valueOf(i+1));
        Spinner spNode = (Spinner) findViewById(R.id.nodeNum);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.my_spinner, nodeNums); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNode.setAdapter(spinnerArrayAdapter);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(comm.getUartConfigured()) {
                    for(int i=0;i<numNodes;i++)if(nodes[i])readNodes++;
                    if(readNodes>0)SendMessage();
                    else Toast.makeText(view.getContext(), "Select at least one Node!" + System.getProperty("line.separator") + "Click on the Nodes you want to read.", Toast.LENGTH_LONG).show();

                }
                else {
                Snackbar.make(view, "Comm not ready yet...", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
//                    Toast.makeText(view.getContext(), "Comm not ready yet...", Toast.LENGTH_SHORT).show();
                    comm.ConfigDev();
                }
            }
        });

        // Create a new DatabaseHelper
        mDataBase = new ModbusDataBase(this, numNodes);
        //mDbHelper = new DX80dbOpenHelper(this);
        //mDataBase.dbUpdate(5,new int[] {16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1});

        // Create a cursor
        c = mDataBase.readRecords();
        mAdapter = new SimpleCursorAdapter(this, R.layout.list_layout, c,
                DX80dbOpenHelper.columns, new int[] {R.id.lv_id,R.id.lv_node,R.id.lv_valor1,R.id.lv_valor2,R.id.lv_valor3,R.id.lv_valor4
                                                    ,R.id.lv_valor5,R.id.lv_valor6,R.id.lv_valor7,R.id.lv_valor8,R.id.lv_valor9
                                                    ,R.id.lv_valor10,R.id.lv_valor11,R.id.lv_valor12,R.id.lv_valor13
                                                    ,R.id.lv_valor14,R.id.lv_valor15,R.id.lv_valor16},0);
        list = (ListView) findViewById(R.id.ListView);
        list.setAdapter(mAdapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                nodes[position] = !nodes[position];
                String item = "Read node " + position + " is " + nodes[position];
                if(nodes[position])list.getAdapter().getView(position,view,parent).setAlpha((float) 1);
                else list.getAdapter().getView(position, view, parent).setAlpha((float)0.30);

                Toast.makeText(getBaseContext(), item, Toast.LENGTH_SHORT).show();

            }
        });

        node = 0;


        rbAuto = (RadioButton) findViewById(R.id.rbAuto);
        rbAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoRead = rbAuto.isChecked();
                Snackbar.make(view, "Auto read is " + autoRead + ". Auto read time is " + autoReadTime / 1000 + " seconds", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                timerHandler.postDelayed(scanNodes, 100);
            }
        });

        rbManual = (RadioButton) findViewById(R.id.rbManual);
        rbManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoRead = !rbManual.isChecked();
                Snackbar.make(view, "Auto read is " + autoRead + ".", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                timerHandler.removeCallbacks(scanNodes);
            }
        });

        etAutoRead = (EditText) findViewById(R.id.etAutoReadTime);
        String autoReadText = String.valueOf(autoReadTime / 1000);
        etAutoRead.setText(autoReadText);
        etAutoRead.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                // you can call or do what you want with your EditText here
                if(s.toString().length()>0) autoReadTime = Integer.parseInt(s.toString())*1000;
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        comm.ConfigDev();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        comm.disconnectFunction();
        timerHandler.removeCallbacks(timeout);
        timerHandler.removeCallbacks(scanNodes);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mUsbReceiver);
        mDataBase.dbClose();
    }

    /***********USB broadcast receiver*******************************************/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //String TAG = "FragL";
            String action = intent.getAction();
            if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
                comm.disconnectFunction();
            }
        }
    };

//    private class readThread  extends Thread
    private class ReadThread extends AsyncTask <Integer, Integer, byte[]> {


        @Override
        protected byte[] doInBackground(Integer... params) {
            int iAvailable = 0;
            int expectedBytes = params[0];

             while (iAvailable < expectedBytes) {
                iAvailable = comm.getAvailable();
//                if(iAvailable == 1) comm.flush(iAvailable);
                if (isCancelled()) break;
            }

            return comm.ReceiveMessage(iAvailable);
        }


        @Override
            protected void onPostExecute(byte[] readData) {
            mDataBase.dbUpdate(((mAddrHi*256 + mAddrLo)/numReg)+1, slave1.ModbusReadReceive(mCmd, mLenHi, mLenLo, readData));
            c = mDataBase.readRecords();
            mAdapter.changeCursor(c);
            mAdapter.notifyDataSetChanged();

            timerHandler.removeCallbacks(timeout);
            node++;
            timerHandler.postDelayed(scanNodes,100);

        }
    }

    public void SendMessage() {

        for(byte i= node;i<=numNodes;i++){
            if(i<numNodes){
                if(nodes[i]) {
                    node = i;
                    int Addr = node * numReg;
                    mAddrLo = (byte) (Addr & 0x00FF);
                    mAddrHi = (byte) (Addr / 256 & 0x00FF);

                    byte[] outData = slave1.ModbusReadSend(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);
                    comm.SendMessage(outData);
                    rt = new ReadThread();
                    rt.execute(expectedBytes);
                    timerHandler.postDelayed(timeout, timeOutTime);
                    break;
                }
            }else {
                node = 0;
                if(autoRead) timerHandler.postDelayed(scanNodes,autoReadTime);
            }
        }


    }

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timeout = new Runnable() {

        @Override
        public void run() {
            rt.cancel(true);
            Toast.makeText(getApplicationContext(), "Read Time Out", Toast.LENGTH_SHORT).show();

            //timerHandler.postDelayed(this, 500);
        }
    };

    Runnable scanNodes = new Runnable() {

        @Override
        public void run() {
            SendMessage();
        }
    };

}



