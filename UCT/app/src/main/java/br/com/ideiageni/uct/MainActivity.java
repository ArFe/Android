package br.com.ideiageni.uct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static final String CURRENT_VIEW = "currentView";

    CommClass comm;

    int numReg = 16;
    int numNodes = 48;
    static int GW = 0;
    static int readMsgs = 6;
    static int numMaxNodes = 48;
    byte node = 0;
    DX80Nodes nodes = new DX80Nodes(numNodes);
    int readNodes = 0;
    boolean autoRead = false;
    int autoReadTime = 2000;
    int ssReadTime = 500;
    int timeOutTime = 2000;

    byte mSlave = 1;
    byte mCmd = 3;
    byte mAddrHi = 0;
    byte mAddrLo = 16;
    int addr = 0;
    final byte mLenHi = 0;
    final byte mLenLo = (byte) numReg;
    int expectedBytes = 37;
    int numRegisters = numReg * numNodes+1;
    Modbus slave1 = new Modbus(mSlave, numRegisters);
    ReadThread rt;
    byte wrNode = 0;
    byte ssNode = 0;
    private boolean SSstarted = false;
    static int ssAddr = 14;
    static byte readMultipleCmd = 3;
    static byte writeSingleCmd = 6;
    static byte ssCmd = 32;

    DX80Adapter arrayAdapter;

    private ViewFlipper viewFlipper;
    View readReg ;
    View writeReg ;
    View connect;
    View siteSur ;

    boolean cnScreen = false;
    boolean rrScreen = false;
    boolean wrScreen = false;
    boolean ssScreen = false;

    private int vfConnect = 0;
    private int vfReadReg = 1;
    private int vfWriteReg = 2;
    private int vfSiteSurvey = 3;
    private int mCurrentView = 0;

    ProgressBar pbStrong;
    ProgressBar pbGood;
    ProgressBar pbWeak;
    ProgressBar pbMissed;

    TextView tvStrong;
    TextView tvGood;
    TextView tvWeak;
    TextView tvMissed;

    ListView list;
    EditText etAutoRead;
    RadioButton rbAuto;
    RadioButton rbManual;

    private float x1,x2;
    static final int MIN_DISTANCE = 50;
    int hScrollIndex = 0;

    String[] columnTags = new String[] {"Node","Value1","Value2","Value3","Value4","Value5","Value6","Value7","Value8",
            "Value9","Value10","Value11","Value12","Value13","Value14","Value15","Value16"};
    int[] columnIds = new int[] {R.id.lv_id, R.id.lv_valor1, R.id.lv_valor2, R.id.lv_valor3, R.id.lv_valor4,
            R.id.lv_valor5, R.id.lv_valor6, R.id.lv_valor7, R.id.lv_valor8, R.id.lv_valor9, R.id.lv_valor10,
            R.id.lv_valor11, R.id.lv_valor12, R.id.lv_valor13, R.id.lv_valor14, R.id.lv_valor15, R.id.lv_valor16};

    View coordinatorLayoutView;
    FloatingActionButton fab;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        viewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        coordinatorLayoutView = findViewById(R.id.snackbarPosition);

        setSupportActionBar(toolbar);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);
        this.registerReceiver(mUsbReceiver, filter);

        // Insere as telas
        setCNscreen();
        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            setCurrentView(savedInstanceState.getInt(CURRENT_VIEW));
        } else {
            // Probably initialize members with default values for a new instance
            setCurrentView(0);
        }

        fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                for(int i=0;i<numNodes;i++) {
                    for (int j = 0; j < numReg + 1; j++) {
                        slave1.setHR(i*numReg+j, i*numReg+j);
                    }
                }
                arrayAdapter.notifyDataSetChanged();

                if(comm!=null) {
                    if (comm.getUartConfigured()) {
                        for (int i = 0; i < numNodes; i++) if (nodes.isReadEnable(i)) readNodes++;
                        if (readNodes > 0) SendMessage();
                        else
                            Toast.makeText(view.getContext(), "Select at least one Node!" + System.getProperty("line.separator") + "Click on the Nodes you want to read.", Toast.LENGTH_LONG).show();

                    } else {
                        Snackbar.make(view, "Comm not ready yet...", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
//                    Toast.makeText(view.getContext(), "Comm not ready yet...", Toast.LENGTH_SHORT).show();
                        comm.ConfigDev();
                    }
                }

                if(viewFlipper.getDisplayedChild() == vfWriteReg) {
                    TextView tView = (TextView) viewFlipper.getChildAt(vfWriteReg).findViewById(R.id.tvIO9);
                    //tView.setText(nodes[0].getValues().toString());
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if(drawer != null) drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if(navigationView != null) navigationView.setNavigationItemSelectedListener(this);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }


    @Override
    public void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        if(comm!=null) comm.ConfigDev();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://br.com.ideiageni.uct/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);

    }

    @Override
    public void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://br.com.ideiageni.uct/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        if(comm!=null) {
            setSSstarted(true);
            if (comm.getUartConfigured()) {
                SendMessage();
            }
            fab.setImageResource(android.R.drawable.ic_media_play);
            comm.disconnectFunction();
        }

        timerHandler.removeCallbacks(timeout);
        timerHandler.removeCallbacks(scanNodes);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mUsbReceiver);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.connect) {
            setCurrentView(vfConnect);
        } else if (id == R.id.readReg) {
            setCurrentView(vfReadReg);
        } else if (id == R.id.writeReg) {
            setCurrentView(vfWriteReg);
        } else if (id == R.id.siteSurvey) {
            setCurrentView(vfSiteSurvey);
        } else if (id == R.id.nav_share) {
            setCurrentView(vfConnect);
        } else if (id == R.id.nav_send) {
            setCurrentView(vfConnect);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawer != null) drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putInt(CURRENT_VIEW, mCurrentView);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    // Ajusta a tela de Connect
    public void setCNscreen() {
        // Se tela Connect não está ajustada
        if (!cnScreen) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            connect = inflater.inflate(R.layout.content_connect, viewFlipper, false);
            vfConnect = viewFlipper.getChildCount();
            viewFlipper.addView(connect, vfConnect);

            final Button connect = (Button) viewFlipper.getChildAt(vfConnect).findViewById(R.id.btConnect);
            if (connect != null) {
                connect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this, ReadRegActivity.class);
//                startActivity(intent);
                        if (comm == null) {
                            comm = new CommClass(getApplicationContext());
                        }

                        if (!comm.getUartConfigured()) {
                            Snackbar.make(view, "Comm not ready yet...", Snackbar.LENGTH_SHORT)
                                    .setAction("Action", null).show();
//                    Toast.makeText(view.getContext(), "Comm not ready yet...", Toast.LENGTH_SHORT).show();
                            comm.ConfigDev();
                            connect.setText(R.string.disconnect);
                        } else connect.setText(R.string.connect);
                    }
                });
            }
            cnScreen = true;
        }
    }

    // Ajusta a tela de Read Registers
    public void setRRscreen() {
        // Se tela Read Registers não está ajustada
        if (!rrScreen) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            readReg = inflater.inflate(R.layout.content_read_reg, viewFlipper, false);
            vfReadReg = viewFlipper.getChildCount();
            viewFlipper.addView(readReg, vfReadReg);

            list = (ListView) viewFlipper.getChildAt(vfReadReg).findViewById(R.id.ListView);
            arrayAdapter = new DX80Adapter(this, R.layout.list_layout, columnTags , columnIds,
                    nodes.getNames(), slave1.getHRArray(), nodes.getReadEnable());

            if (list != null) {
                list.setAdapter(arrayAdapter);
                list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                            long id) {
                        nodes.setReadEnable(position, !nodes.isReadEnable(position));
                        String item = "Read node " + position + " is " + nodes.isReadEnable(position);
                        arrayAdapter.notifyDataSetChanged();
                        Toast.makeText(getBaseContext(), item, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
                list.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch(event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                x1 = event.getX();
                                break;
                            case MotionEvent.ACTION_MOVE:// .ACTION_UP:
                                x2 = event.getX();
                                float deltaX = x2 - x1;
                                float absDeltaX = Math.abs(deltaX);
                                if (absDeltaX > MIN_DISTANCE) {
                                    // Left to Right swipe action
                                    if (x2 > x1) {
                                        if(hScrollIndex>(int)absDeltaX/MIN_DISTANCE) hScrollIndex = hScrollIndex - (int) absDeltaX/MIN_DISTANCE;
                                        else hScrollIndex = 0;
                                    }
                                    // Right to left swipe action
                                    else {
                                        if(hScrollIndex<numReg-5-(int) absDeltaX/MIN_DISTANCE) hScrollIndex = hScrollIndex + (int) absDeltaX/MIN_DISTANCE;
                                        else hScrollIndex = numReg-5;
                                    }
                                    x1 = x2;
                                    arrayAdapter.setHScrollIndex(hScrollIndex);
                                    arrayAdapter.notifyDataSetChanged();
                                    scrollHeader(hScrollIndex);
                                }
                                else {
                                    // consider as something else - a screen tap for example
                                }
                                break;
                        }
                        return false;
                    }
                });
            }

            rbAuto = (RadioButton) viewFlipper.getChildAt(vfReadReg).findViewById(R.id.rbAuto);
            rbAuto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    autoRead = rbAuto.isChecked();
                    Snackbar.make(view, "Auto read is " + autoRead + ". Auto read time is " + autoReadTime / 1000 + " seconds", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                    if(comm != null) timerHandler.postDelayed(scanNodes, 100);
                }
            });

            rbManual = (RadioButton) viewFlipper.getChildAt(vfReadReg).findViewById(R.id.rbManual);
            rbManual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    autoRead = !rbManual.isChecked();
                    Snackbar.make(view, "Auto read is " + autoRead + ".", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                    timerHandler.removeCallbacks(scanNodes);
                }
            });

            etAutoRead = (EditText) viewFlipper.getChildAt(vfReadReg).findViewById(R.id.etAutoReadTime);
            String autoReadText = String.valueOf(autoReadTime / 1000);
            etAutoRead.setText(autoReadText);
            etAutoRead.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                    // you can call or do what you want with your EditText here
                    if (s.toString().length() > 0)
                        autoReadTime = Integer.parseInt(s.toString()) * 1000;
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            rrScreen = true;
        }
    }

    // Ajusta a tela de Write Registers
    public void setWRscreen() {
        // Se tela Write Registers não está ajustada
        if (!wrScreen) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            writeReg = inflater.inflate(R.layout.content_write_reg, viewFlipper, false);
            vfWriteReg = viewFlipper.getChildCount();
            viewFlipper.addView(writeReg, vfWriteReg);

            ArrayList<String> nodeNums = new ArrayList<>();
            nodeNums.add("GW");
            for (int i = 1; i <= numNodes; i++) nodeNums.add(String.valueOf(i));
            Spinner spNode = (Spinner) viewFlipper.getChildAt(vfWriteReg).findViewById(R.id.nodeNumWR);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.my_spinner, nodeNums); //selected item will look like a spinner set from XML
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spNode.setAdapter(spinnerArrayAdapter);

            spNode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapter, View v, int position, long arg3) {
                    wrNode = Byte.parseByte(String.valueOf(position));
                    slave1.setHR(0,(int)wrNode);

                    Toast.makeText(getApplicationContext(), "Write node: " + wrNode, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }
            });

            wrScreen = true;
        }
    }

    // Ajusta a tela de Site Survey
    public void setSSscreen() {
        // Se tela Site Survey não está ajustada
        if (!ssScreen) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            siteSur = inflater.inflate(R.layout.content_site_survey, viewFlipper, false);
            vfSiteSurvey = viewFlipper.getChildCount();
            viewFlipper.addView(siteSur, vfSiteSurvey);

            pbStrong = (ProgressBar) viewFlipper.getChildAt(vfSiteSurvey).findViewById(R.id.pbStrong);
            pbGood = (ProgressBar) viewFlipper.getChildAt(vfSiteSurvey).findViewById(R.id.pbGood);
            pbWeak = (ProgressBar) viewFlipper.getChildAt(vfSiteSurvey).findViewById(R.id.pbWeak);
            pbMissed = (ProgressBar) viewFlipper.getChildAt(vfSiteSurvey).findViewById(R.id.pbMissed);

            if (pbStrong != null) {
                pbStrong.setProgress(80);
                pbStrong.setScaleY(10f);
            }
            if (pbGood != null) {
                pbGood.setScaleY(10f);
                pbGood.setProgress(60);
            }
            if (pbWeak != null) {
                pbWeak.setScaleY(10f);
                pbWeak.setProgress(40);
            }
            if (pbMissed != null) {
                pbMissed.setScaleY(10f);
                pbMissed.setProgress(20);
            }

            tvStrong = (TextView) viewFlipper.getChildAt(vfSiteSurvey).findViewById(R.id.tvStrong);
            tvGood = (TextView) viewFlipper.getChildAt(vfSiteSurvey).findViewById(R.id.tvGood);
            tvWeak = (TextView) viewFlipper.getChildAt(vfSiteSurvey).findViewById(R.id.tvWeak);
            tvMissed = (TextView) viewFlipper.getChildAt(vfSiteSurvey).findViewById(R.id.tvMissed);

            ArrayList<String> nodeNums = new ArrayList<>();
            for (int i = 1; i <= numNodes; i++) nodeNums.add(String.valueOf(i));
            Spinner spNode = (Spinner) viewFlipper.getChildAt(vfSiteSurvey).findViewById(R.id.nodeNumSS);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.my_spinner, nodeNums); //selected item will look like a spinner set from XML
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spNode.setAdapter(spinnerArrayAdapter);

            spNode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapter, View v, int position, long arg3) {
                    ssNode = Byte.parseByte((String) adapter.getItemAtPosition(position));
                    Toast.makeText(getApplicationContext(), "Site Survey node: " + ssNode, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }
            });


            ssScreen = true;
        }
   }


    /***********
     * USB broadcast receiver
     *******************************************/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        //String TAG = "FragL";
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            comm.disconnectFunction();
        }
        }
    };


    public void SendMessage() {
        if(getCurrentView() == vfReadReg) {
            for (byte i = node; i <= numNodes; i++) {
                if (i < numNodes) {
                    if (nodes.isReadEnable(i)) {
                        node = i;
                        addr = node * numReg;
                        mAddrLo = (byte) (addr & 0x00FF);
                        mAddrHi = (byte) (addr / 256 & 0x00FF);

                        byte[] outData = slave1.readSend(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);
                        comm.SendMessage(outData);
                        rt = new ReadThread();
                        rt.execute(expectedBytes);
                        timerHandler.postDelayed(timeout, timeOutTime);
                        break;
                    }
                } else {
                    //Reinicia o ciclo. Node igual a zero e post delay para auto read se habilitado
                    node = 0;
                    if (autoRead) timerHandler.postDelayed(scanNodes, autoReadTime);
                }
            }
        } else if(getCurrentView() == vfSiteSurvey) {
            if(isSSstarted()) {
                node = 0;
                mAddrLo = 0;
                mAddrHi = 0;
                mCmd = readMultipleCmd;

                byte[] outData = slave1.readSend(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);
                comm.SendMessage(outData);
                rt = new ReadThread();
                rt.execute(expectedBytes);
                timerHandler.postDelayed(timeout, timeOutTime);
                timerHandler.postDelayed(scanNodes, ssReadTime);
            } else {
                mCmd = writeSingleCmd;
                if(!isSSstarted()) node=0;
                else node = ssNode;
                int addr = ssAddr;
                mAddrLo = (byte) (addr & 0x00FF);
                mAddrHi = (byte) (addr / 256 & 0x00FF);
                byte[] data = {ssCmd,node};
                byte[] outData = slave1.writeSend(mCmd , mAddrHi, mAddrLo, data);
                expectedBytes = outData.length-1;
                comm.SendMessage(outData);
                rt = new ReadThread();
                rt.execute(expectedBytes);
                timerHandler.postDelayed(timeout, timeOutTime);

            }
        }
    }

    // private class readThread  extends Thread
    public class ReadThread extends AsyncTask<Integer, Integer, byte[]> {

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
            int commResult = slave1.readReceive(mCmd, mLenHi, mLenLo, readData, mAddrHi, mAddrLo);
            Snackbar.make(coordinatorLayoutView, slave1.getError(commResult), Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
//            Toast.makeText(getApplicationContext(), slave1.getError(commResult), Toast.LENGTH_SHORT).show();
            if(getCurrentView() == vfReadReg) arrayAdapter.notifyDataSetChanged();
            else if(getCurrentView()== vfSiteSurvey) updateSSValues();
            timerHandler.removeCallbacks(timeout);
            node++;
            timerHandler.postDelayed(scanNodes, 100);
        }
    }

    // runs without a timer by reposting this handler at the end of the runnable
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


    public void updateRRValues () {
        //for(int i=0;i<numNodes;i++) nodes[i].setValues(Arrays.copyOfRange(modbusArray,i*16+1,(i+1)*16));
    }

    public void updateWRValues () {

    }

    public void updateSSValues (){
        int[] SSdata = slave1.getHR(GW*numReg+readMsgs,2);
        int strong = SSdata[1]/256;
        int good = SSdata[1] & 0x00FF;
        int weak = SSdata[0]/256;
        int missed = SSdata[0] & 0x00FF;
        String tmpStrong = String.valueOf(strong)+"%";
        String tmpGood = String.valueOf(good)+"%";
        String tmpWeak = String.valueOf(weak)+"%";
        String tmpMissed = String.valueOf(missed)+"%";

        pbStrong.setProgress(strong);
        pbGood.setProgress(good);
        pbWeak.setProgress(weak);
        pbMissed.setProgress(missed);

        tvStrong.setText(tmpStrong);
        tvGood.setText(tmpGood);
        tvWeak.setText(tmpWeak);
        tvMissed.setText(tmpMissed);

    }

    public void startSS(){
        if(!isSSstarted()){


        }

    }


    public void setCurrentView(int currentView) {
        this.mCurrentView = currentView;
        if(!rrScreen) setRRscreen();
        if(!wrScreen) setWRscreen();
        if(!ssScreen) setSSscreen();

        viewFlipper.setDisplayedChild(this.mCurrentView);
        Toast.makeText(getApplicationContext(), "Current View" + mCurrentView, Toast.LENGTH_SHORT).show();

    }

    public int getCurrentView() {
        return this.mCurrentView;
    }

    public boolean isSSstarted() {
        return SSstarted;
    }

    public void setSSstarted(boolean SSstarted) {
        this.SSstarted = SSstarted;
    }

    public byte getNode() {

        return node;
    }

    public void setNode(byte node) {
        this.node = node;
    }

    private void scrollHeader(int index) {
        AppCompatTextView[] tv = new AppCompatTextView[numReg];
        LinearLayout ll = (LinearLayout) viewFlipper.getChildAt(vfReadReg).findViewById(R.id.tvLinearLayoutheader);
        for (int i = 1; i < ll.getChildCount(); i++) {
            if (ll.getChildAt(i).getClass() == AppCompatTextView.class) {
                tv[i-1] = (AppCompatTextView)ll.getChildAt(i);
                String tempStr = "";
                if(index+i<=numReg) tempStr = String.valueOf(index+i);
                tv[i-1].setText(tempStr);
            }
        }
    }

}


