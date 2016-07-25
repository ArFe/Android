package br.com.ideiageni.uct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String CURRENT_VIEW = "currentView";
    private static final int GW = 0;
    private static final int readMsgs = 6;
    private static final int numMaxNodes = 48;
    public static final int numReg = 16;
    private final static String[] columnTags = new String[] {"Node","Value1","Value2","Value3","Value4","Value5","Value6","Value7","Value8",
            "Value9","Value10","Value11","Value12","Value13","Value14","Value15","Value16"};
    private final static int[] columnIds = new int[] {R.id.lv_id, R.id.lv_valor1, R.id.lv_valor2, R.id.lv_valor3, R.id.lv_valor4,
            R.id.lv_valor5, R.id.lv_valor6, R.id.lv_valor7, R.id.lv_valor8, R.id.lv_valor9, R.id.lv_valor10,
            R.id.lv_valor11, R.id.lv_valor12, R.id.lv_valor13, R.id.lv_valor14, R.id.lv_valor15, R.id.lv_valor16};

    private CommClass comm;
    private byte mSlave = 1;
    private int numNodes = numMaxNodes;
    private int numRegisters = numReg * numNodes+1;
    public DX80Nodes nodes = new DX80Nodes(numNodes);
    public Modbus slave1 = new Modbus(mSlave, numRegisters);
    public ModbusMaster master;
    public AppsControl control;
    private byte wrNode = 0;
    private byte ssNode = 0;
    private byte vtNode = 0;
    private int[] writeReg = new int[numReg];
    private boolean[] isWriteRegEnable = new boolean[numReg];
    private boolean isSsEnable = false;

    private DX80Adapter arrayAdapter;

    private ViewFlipper viewFlipper;

    private boolean cnScreen = false;
    private boolean rrScreen = false;
    private boolean wrScreen = false;
    private boolean ssScreen = false;
    private boolean vtScreen = false;

    private int appConnect = 0;
    private int appReadReg = 999;
    private int appWriteReg = 999;
    private int appSiteSurvey = 999;
    private int appVibTemp = 999;
    private int mCurrentApp = 0;
    private int[] backControlArray= new int[100];
    private int backControl = 0;

    private ProgressBar pbStrong;
    private ProgressBar pbGood;
    private ProgressBar pbWeak;
    private ProgressBar pbMissed;

    private TextView tvStrong;
    private TextView tvGood;
    private TextView tvWeak;
    private TextView tvMissed;

    private RadioButton rbAuto;
    private RadioButton rbManual;

    private float x1,x2;
    private static final int MIN_DISTANCE = 30;
    private int hScrollIndex = 0;

    public View coordinatorLayoutView;
    private FloatingActionButton fab;
    private OnScreenLog log;
    GraphView graph;
    private int graphCnt;

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
        control = new AppsControl(this);
        setSupportActionBar(toolbar);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);
        this.registerReceiver(mUsbReceiver, filter);

        // Insert Connection Screen
        comm = new CommClass(getApplicationContext());
        master = new ModbusMaster(comm, slave1);
        setCNscreen();
        log = new OnScreenLog(this, R.id.mnView);
        // Enable GW reading
        nodes.setReadEnable(GW, true);
        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            setCurrentApp(savedInstanceState.getInt(CURRENT_VIEW),false);
        } else {
            // Probably initialize members with default values for a new instance
            setCurrentApp(0,false);
        }

        fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(comm!=null) {
                    if (!comm.getUartConfigured()) {
                        Snackbar.make(view, "Comm not ready yet...", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                        comm.ConfigDev();
                    } else {
                        control.setRunningTrue();
                        setSsEnable(!isSsEnable());
                        Snackbar.make(view, "Running is " + control.isRunning(), Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();                   }
                } else {
                    Snackbar.make(view, "Comm Null...", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
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
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        control.onStop();
        if(comm!=null) {
            if (comm.getUartConfigured()) {
                master.stopComm();
            }
            comm.disconnectFunction();
        }
        this.unregisterReceiver(mUsbReceiver);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (backControl>0) {
            backControl--;
            setCurrentApp(backControlArray[backControl], true);
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
            setCurrentApp(appConnect, false);
        } else if (id == R.id.readReg) {
            if(!rrScreen) setRRscreen();
            setCurrentApp(appReadReg, false);
        } else if (id == R.id.writeReg) {
            if(!wrScreen) setWRscreen();
            setCurrentApp(appWriteReg, false);
        } else if (id == R.id.siteSurvey) {
            if(!ssScreen) setSSscreen();
            setCurrentApp(appSiteSurvey, false);
        } else if (id == R.id.vibTemp) {
            if(!vtScreen) setVTscreen();
            setCurrentApp(appVibTemp, false);
        } else if (id == R.id.nav_share) {
            log.setLogVisible(true);
        } else if (id == R.id.nav_send) {
            log.setLogVisible(false);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawer != null) drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putInt(CURRENT_VIEW, mCurrentApp);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    // Ajusta a tela de Connect
    public void setCNscreen() {
        View connect;

        // Se tela Connect não está ajustada
        if (!cnScreen) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            connect = inflater.inflate(R.layout.content_connect, viewFlipper, false);
            appConnect = viewFlipper.getChildCount();
            viewFlipper.addView(connect, appConnect);

            final Button btnConnect = (Button) viewFlipper.getChildAt(appConnect).findViewById(R.id.btConnect);
            if (btnConnect != null) {
                btnConnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        control.setRunningTrue();
                        if (comm == null) {
                            comm = new CommClass(getApplicationContext());
                            master = new ModbusMaster(comm, slave1);
                        }

                        if (!comm.getUartConfigured()) {
                            Snackbar.make(view, "Comm not ready yet...", Snackbar.LENGTH_SHORT)
                                    .setAction("Action", null).show();
                            comm.ConfigDev();
                            btnConnect.setText(R.string.connect);
                        } else btnConnect.setText(R.string.disconnect);
                    }
                });
            }
            cnScreen = true;
        }
    }

    // Ajusta a tela de Read Registers
    public void setRRscreen() {
        View readReg ;
        ListView list;
        EditText etAutoRead;

        // Se tela Read Registers não está ajustada
        if (!rrScreen) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            readReg = inflater.inflate(R.layout.content_read_reg, viewFlipper, false);
            appReadReg = viewFlipper.getChildCount();
            viewFlipper.addView(readReg, appReadReg);

            list = (ListView) viewFlipper.getChildAt(appReadReg).findViewById(R.id.ListView);
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

            rbAuto = (RadioButton) viewFlipper.getChildAt(appReadReg).findViewById(R.id.rbAuto);
            rbAuto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    control.setAutoRead(rbAuto.isChecked());
                    Snackbar.make(view, "Auto read is " + control.isAutoRead() + ". Auto read time is " +
                            control.getAutoReadTime() / 1000 + " seconds", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            });

            rbManual = (RadioButton) viewFlipper.getChildAt(appReadReg).findViewById(R.id.rbManual);
            rbManual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    control.setAutoRead(!rbManual.isChecked());
                    Snackbar.make(view, "Auto read is " + control.isAutoRead() + ".", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            });

            etAutoRead = (EditText) viewFlipper.getChildAt(appReadReg).findViewById(R.id.etAutoReadTime);
            String autoReadText = String.valueOf(control.getAutoReadTime() / 1000);
            etAutoRead.setText(autoReadText);
            etAutoRead.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                    // you can call or do what you want with your EditText here
                    if (s.toString().length() > 0)
                        control.setAutoReadTime(Integer.parseInt(s.toString()) * 1000);
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
        View writeReg;
        // Se tela Write Registers não está ajustada
        if (!wrScreen) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            writeReg = inflater.inflate(R.layout.content_write_reg, viewFlipper, false);
            appWriteReg = viewFlipper.getChildCount();
            viewFlipper.addView(writeReg, appWriteReg);

            ArrayList<String> nodeNums = new ArrayList<>();
            nodeNums.add("GW");
            for (int i = 1; i <= numNodes; i++) nodeNums.add(String.valueOf(i));
            Spinner spNode = (Spinner) viewFlipper.getChildAt(appWriteReg).findViewById(R.id.nodeNumWR);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.my_spinner, nodeNums); //selected item will look like a spinner set from XML
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spNode.setAdapter(spinnerArrayAdapter);

            spNode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapter, View v, int position, long arg3) {
                    setWrNode(Byte.parseByte(String.valueOf(position)));
                    slave1.setHR(0,(int)getWrNode());

                    Toast.makeText(getApplicationContext(), "Write node: " + getWrNode(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }
            });

            AppCompatCheckBox[] cb = new AppCompatCheckBox[numReg];
            AppCompatEditText[] et = new AppCompatEditText[numReg];
            TableLayout tl = (TableLayout) viewFlipper.getChildAt(appWriteReg).findViewById(R.id.tableLayout);
            int indexCB = 0;
            int indexET = 0;
            for (int i = 0; i < tl.getChildCount(); i++) {
                if (tl.getChildAt(i).getClass() == TableRow.class) {
                    TableRow tr = (TableRow) tl.getChildAt(i);
                    for (int j = 0; j < tr.getChildCount(); j++) {
                        if (tr.getChildAt(j).getClass() == AppCompatCheckBox.class) {
                            cb[indexCB] = (AppCompatCheckBox) tr.getChildAt(j);
                            cb[indexCB].setTag(Integer.valueOf((String)cb[indexCB].getTag())-1);
                            indexCB++;
                        } else if (tr.getChildAt(j).getClass() == AppCompatEditText.class) {
                            et[indexET] = (AppCompatEditText) tr.getChildAt(j);
                            et[indexET].setTag(Integer.valueOf((String)et[indexET].getTag())-1);
                            indexET++;
                        }

                    }
                }
            }

            for(int i=0;i<numReg; i++){
                if(cb[i] != null) cb[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        buttonView.getTag();
                        isWriteRegEnable[(int)buttonView.getTag()] = isChecked;
                        log.log("status:" + isChecked + " Tag: " + (int)buttonView.getTag());
                    }
                });
                if(et[i] != null) et[i].addTextChangedListener(new ArrayTextWatcher((int) et[i].getTag()));
            }
            wrScreen = true;
        }
    }

    public class ArrayTextWatcher implements TextWatcher {
        private int mIndex = 0;
        public ArrayTextWatcher(int index) {
            this.mIndex = index;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().length() > 0) {
                try {
                    setWriteReg(Integer.parseInt(s.toString()), mIndex);
                    log.log(mIndex + " - " + Integer.parseInt(s.toString()));
                } catch (Exception e){
                    log.log("Not interger. " + e.toString());
                }
            }
        }

    }

    // Ajusta a tela de Site Survey
    public void setSSscreen() {
         // Se tela Site Survey não está ajustada
        if (!ssScreen) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View siteSur = inflater.inflate(R.layout.content_site_survey, viewFlipper, false);
            appSiteSurvey = viewFlipper.getChildCount();
            viewFlipper.addView(siteSur, appSiteSurvey);

            pbStrong = (ProgressBar) viewFlipper.getChildAt(appSiteSurvey).findViewById(R.id.pbStrong);
            pbGood = (ProgressBar) viewFlipper.getChildAt(appSiteSurvey).findViewById(R.id.pbGood);
            pbWeak = (ProgressBar) viewFlipper.getChildAt(appSiteSurvey).findViewById(R.id.pbWeak);
            pbMissed = (ProgressBar) viewFlipper.getChildAt(appSiteSurvey).findViewById(R.id.pbMissed);

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

            tvStrong = (TextView) viewFlipper.getChildAt(appSiteSurvey).findViewById(R.id.tvStrong);
            tvGood = (TextView) viewFlipper.getChildAt(appSiteSurvey).findViewById(R.id.tvGood);
            tvWeak = (TextView) viewFlipper.getChildAt(appSiteSurvey).findViewById(R.id.tvWeak);
            tvMissed = (TextView) viewFlipper.getChildAt(appSiteSurvey).findViewById(R.id.tvMissed);

            ArrayList<String> nodeNums = new ArrayList<>();
            for (int i = 1; i <= numNodes; i++) nodeNums.add(String.valueOf(i));
            Spinner spNode = (Spinner) viewFlipper.getChildAt(appSiteSurvey).findViewById(R.id.nodeNumSS);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.my_spinner, nodeNums); //selected item will look like a spinner set from XML
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spNode.setAdapter(spinnerArrayAdapter);

            spNode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapter, View v, int position, long arg3) {
                    setSsNode(Byte.parseByte((String) adapter.getItemAtPosition(position)));
                    Toast.makeText(getApplicationContext(), "Site Survey node: " + getSsNode(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }
            });


            ssScreen = true;
        }
   }

    // Ajusta a tela de Vibração e Temperatura
    public void setVTscreen() {
        View vibTemp ;
        EditText etAutoRead;

        // Se tela Read Registers não está ajustada
        if (!vtScreen) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            vibTemp = inflater.inflate(R.layout.content_vt, viewFlipper, false);
            appVibTemp = viewFlipper.getChildCount();
            viewFlipper.addView(vibTemp, appVibTemp);
            log.log("App Vib Temp: " + appVibTemp);

            graph = (GraphView) viewFlipper.getChildAt(appVibTemp).findViewById(R.id.vtGraph);
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                    new DataPoint(0, 1),
                    new DataPoint(1, 5),
                    new DataPoint(2, 3),
                    new DataPoint(3, 2),
                    new DataPoint(4, 6)
            });
            graph.addSeries(series);
            graphCnt = 5;
            rbAuto = (RadioButton) viewFlipper.getChildAt(appVibTemp).findViewById(R.id.rbAuto);
            rbAuto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    control.setAutoRead(rbAuto.isChecked());
                    Snackbar.make(view, "Auto read is " + control.isAutoRead() + ". Auto read time is " +
                            control.getAutoReadTime() / 1000 + " seconds", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            });

            rbManual = (RadioButton) viewFlipper.getChildAt(appVibTemp).findViewById(R.id.rbManual);
            rbManual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    control.setAutoRead(!rbManual.isChecked());
                    Snackbar.make(view, "Auto read is " + control.isAutoRead() + ".", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            });

            etAutoRead = (EditText) viewFlipper.getChildAt(appVibTemp).findViewById(R.id.etAutoReadTime);
            String autoReadText = String.valueOf(control.getAutoReadTime() / 1000);
            etAutoRead.setText(autoReadText);
            etAutoRead.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                    // you can call or do what you want with your EditText here
                    if (s.toString().length() > 0)
                        control.setAutoReadTime(Integer.parseInt(s.toString()) * 1000);
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });


            ArrayList<String> nodeNums = new ArrayList<>();
            for (int i = 1; i <= numNodes; i++) nodeNums.add(String.valueOf(i));
            Spinner spNode = (Spinner) viewFlipper.getChildAt(appVibTemp).findViewById(R.id.nodeNumVT);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.my_spinner, nodeNums); //selected item will look like a spinner set from XML
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spNode.setAdapter(spinnerArrayAdapter);

            spNode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapter, View v, int position, long arg3) {
                    setVtNode(Byte.parseByte((String) adapter.getItemAtPosition(position)));
                    Toast.makeText(getApplicationContext(), "VT node: " + getVtNode(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }
            });
            vtScreen = true;
        }
    }

    public void updateRRScreen () {
        //for(int i=0;i<numNodes;i++) nodes[i].setValues(Arrays.copyOfRange(modbusArray,i*16+1,(i+1)*16));
    }

    public void updateWRScreen () {

    }

    public void updateSSScreen (){
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

    public void updateVTScreen () {
        graph = (GraphView) viewFlipper.getChildAt(appVibTemp).findViewById(R.id.vtGraph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(graphCnt, slave1.getHR(getVtNode()*16+2))
        });
        graph.addSeries(series);
        graph.onDataChanged(false, false);
        graphCnt++;
        log.log("Count=" + graphCnt + " Data=" + slave1.getHR(getVtNode()*16+2));
    }

    public void setCurrentApp(int currentApp, boolean back) {
        backControlArray[backControl] = this.mCurrentApp;
        if(!back) backControl++;
        this.mCurrentApp = currentApp;

        viewFlipper.setDisplayedChild(this.mCurrentApp);
        control.setCurrentApp(this.mCurrentApp);
        setSsEnable(false);

        //log.log(System.getProperty("line.separator") + "App " + this.mCurrentApp);
        //tvLog.append(System.getProperty("line.separator") + "App " + this.mCurrentApp);

//        Toast.makeText(getApplicationContext(), "Current View" + mCurrentApp, Toast.LENGTH_SHORT).show();

    }

    public int getCurrentApp() {
        return this.mCurrentApp;
    }

    private void scrollHeader(int index) {
        AppCompatTextView[] tv = new AppCompatTextView[numReg];
        LinearLayout ll = (LinearLayout) viewFlipper.getChildAt(appReadReg).findViewById(R.id.tvLinearLayoutheader);
        for (int i = 1; i < ll.getChildCount(); i++) {
            if (ll.getChildAt(i).getClass() == AppCompatTextView.class) {
                tv[i-1] = (AppCompatTextView)ll.getChildAt(i);
                String tempStr = "";
                if(index+i<=numReg) tempStr = String.valueOf(index+i);
                tv[i-1].setText(tempStr);
            }
        }
    }

    public int getAppConnect() {
        return appConnect;
    }

    public void setAppConnect(int appConnect) {
        this.appConnect = appConnect;
    }

    public int getAppReadReg() {
        return appReadReg;
    }

    public void setAppReadReg(int app) {
        this.appReadReg = app;
    }

    public int getAppWriteReg() {
        return appWriteReg;
    }

    public void setAppWriteReg(int app) {
        this.appWriteReg = app;
    }

    public int getAppSiteSurvey() {
        return appSiteSurvey;
    }

    public void setAppSiteSurvey(int app) {
        this.appSiteSurvey = app;
    }

    public int getAppVibTemp() {
        return appVibTemp;
    }

    public void setAppVibTemp(int app) {
        this.appVibTemp = app;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    public byte getWrNode() {
        return wrNode;
    }

    public void setWrNode(byte node) {
        this.wrNode = node;
    }

    public byte getSsNode() {
        return ssNode;
    }

    public void setSsNode(byte node) {
        this.ssNode = node;
    }

    public byte getVtNode() {
        return vtNode;
    }

    public void setVtNode(byte node) {
        this.vtNode = node;
    }

    public void notifyDataSetChanged () {
        if(getCurrentApp() == appReadReg) arrayAdapter.notifyDataSetChanged();
        else if(getCurrentApp() == appVibTemp) updateVTScreen();
    }

    public boolean getIsWriteRegEnable(int index) {
        return isWriteRegEnable[index];
    }

    public void setIsWriteRegEnable(boolean isWriteRegEnable, int index) {
        this.isWriteRegEnable[index] = isWriteRegEnable;
    }

    public int getWriteReg(int index) {
        return writeReg[index];
    }

    public void setWriteReg(int writeReg, int index) {
        this.writeReg[index] = writeReg;
    }

    public boolean isSsEnable() {
        return isSsEnable;
    }

    public void setSsEnable(boolean ssEnable) {
        isSsEnable = ssEnable;
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
                control.onStop();
                if(comm!=null) {
                    if (comm.getUartConfigured()) {
                        master.stopComm();
                    }
                    comm.disconnectFunction();
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                control.onStart();
                if(comm!=null && !comm.getUartConfigured()) comm.ConfigDev();
            }
        }
    };


}


