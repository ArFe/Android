package br.com.ideiageni.uct;

import android.os.Handler;
import android.support.design.widget.Snackbar;

/**
 * Created by ariel on 05/07/2016.
 */
public class AppsControl {

    private static final int HOLD = 0;
    private static final int CONNECT = 1;
    private static final int READ_REGISTERS = 2;
    private static final int WRITE_REGISTERS = 3;
    private static final int SITE_SURVEY = 4;
    private static final int START = 5;
    private static final int RUN = 6;
    private static final int WAIT_RETURN = 7;
    private static final int WAIT_AUTO_READ = 8;
    private static final int STOP = 9;
    private static final int START_SS = 10;
    private static final int STOP_SS = 11;
    private static final int WAIT_SS = 12;
    private static final int READ_SS = 13;
    private static final int WAIT_START = 14;
    private static final int WAIT_READ_SS = 15;
    private static final int WAIT_DONE = 16;
    private static final int DISCONNECT = 17;
    private static final int CREATE_DEV_LIST = 18;
    private static final int CONFIG = 19;
    private static final int CONFIGURED = 20;
    private static final int VIBRATION_TEMPERATURE = 21;
    private static final int IO = 22;
    private static final int TIMEOUT = 99;

    private int cycleTime = 100;
    private int reportTime = 1000;
    private int timeoutTime = 10000;

    private static final byte ssCmd = 32;
    static int ssAddr = 14;

    private boolean ssEnable = false;
    private int currentApp = 999;

    private int mainState = 0;
    private int appState = 0;
    private MainActivity mainActivity;
    public ModbusMaster master;

    private byte node = 0;
    private byte wrReg = 0;
    private final byte mLenHi = 0;
    private byte mLenLo = 16;
    private boolean autoReadRR = false;
    private boolean autoReadVT = false;
    private int autoReadTime = 2000;
    private int ssReadTime = 500;
    private boolean scan = false;
    private boolean newApp = false;
    private boolean running = false;
    private boolean timeout = false;
    private boolean commConfigured = false;
    private int cntTimeout = 0;
    private boolean started = false;

    private int counter = 0;

    private OnScreenLog log = new OnScreenLog();

     public AppsControl (MainActivity context){
         this.mainActivity = context;
         onStart();
    }

    public void main(){
        int app;

        switch (mainState) {
            case HOLD:
                if(isRunning()) mainState = START;
                timerHandler.removeCallbacks(rTimeout);
                break;

            case START:
                log.log("START");
                app = getCurrentApp();
                setNewApp(false);
                mainActivity.keepScreenOn();
                mainActivity.setFABPause();
                if (app == mainActivity.getAppConnect()) mainState = CONNECT;
                else if (app == mainActivity.getAppReadReg()) mainState = READ_REGISTERS;
                else if (app == mainActivity.getAppWriteReg()) mainState = WRITE_REGISTERS;
                else if (app == mainActivity.getAppSiteSurvey()) mainState = SITE_SURVEY;
                else if (app == mainActivity.getAppVibTemp()) mainState = VIBRATION_TEMPERATURE;
                else if (app == mainActivity.getAppIO()) mainState = IO;
//                log.log("App = " + app + " Vib temp = " + mainActivity.getAppVibTemp());
                timerHandler.postDelayed(rTimeout, timeoutTime);
                break;

            case CONNECT:
                if (appCN()) mainState = STOP;
                if(timeout) mainState = TIMEOUT;
                break;

            case READ_REGISTERS:
                if (appRR()) mainState = STOP;
                if(timeout) mainState = TIMEOUT;
                break;

            case WRITE_REGISTERS:
                if (appWR()) mainState = STOP;
                if(timeout) mainState = TIMEOUT;
                break;

            case SITE_SURVEY:
                if (appSS()) mainState = STOP;
                if(timeout) mainState = TIMEOUT;
                break;

            case VIBRATION_TEMPERATURE:
                if (appVT()) mainState = STOP;
                if(timeout) mainState = TIMEOUT;
                break;

            case IO:
                if (appIO()) mainState = STOP;
                if(timeout) mainState = TIMEOUT;
                break;

            case STOP:
                mainState = HOLD;
                setRunning(false);
                mainActivity.clearKeepScreenOn();
                mainActivity.setFABPlay();
                break;

            case TIMEOUT:
                mainState = STOP;
                timeout = false;
                cntTimeout++;
                log.log("Timeout " + cntTimeout);
                if(cntTimeout > mainActivity.getNumRetries()) onStop();
                break;

            default:
                mainState = HOLD;
                setRunning(false);
        }

    }

    private boolean appCN(){
        boolean result = false;
        switch (appState) {
            case START:
                if(commConfigured) appState = DISCONNECT;
                else appState = CREATE_DEV_LIST;
                break;

            case CREATE_DEV_LIST:
                if(mainActivity.usbComm.getDevCount() <= 0) mainActivity.usbComm.createDeviceList();
                else appState = CONNECT;
                break;

            case CONNECT:
                if((mainActivity.usbComm.getftDevNull()||!mainActivity.usbComm.ftDev.isOpen())
                        && mainActivity.usbComm.getDevCount() > 0) mainActivity.usbComm.connectFunction();
                else appState = CONFIG;
                break;

            case CONFIG:
                if (!(mainActivity.usbComm.isUartConfigured() && !mainActivity.usbComm.getftDevNull()
                        && mainActivity.usbComm.getDevCount() > 0)) mainActivity.usbComm.ConfigPort();
                else appState = CONFIGURED;
                break;

            case CONFIGURED:
                if(mainActivity.usbComm.getUartConfigured()) {
                    commConfigured = true;
                    appState = START;
                    mainActivity.setFABCloseClearCancel();
                    result = true;
                }

                break;

            case DISCONNECT:
                mainActivity.usbComm.disconnectFunction();
                commConfigured = false;
                appState = START;
                mainActivity.setFABPlay();
                result = true;

            default:
                appState = START;
        }

        return result;
    }

    private boolean appRR(){
        boolean result = false;
        switch (appState) {
            case START:
                node = 0;
                int readNodes = 0;
                for (int i = 0; i < mainActivity.getNumNodes(); i++) {
                    if (mainActivity.nodes.isReadEnable(i)) readNodes++;
                }
                if (readNodes > 0) appState = RUN;
                else {
                    appState = STOP;
                    Snackbar.make(mainActivity.coordinatorLayoutView, "Select at least one Node!"
                            + System.getProperty("line.separator")
                            + "Long click on the Nodes you want to read.", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
                break;

            case RUN:
                for (byte i = node; i <= mainActivity.getNumNodes(); i++) {
                    if (i < mainActivity.getNumNodes()) {
                        if (mainActivity.nodes.isReadEnable(i)) {
                            node = i;
                            mLenLo = 16;
                            int addr = node * MainActivity.numReg;
                            byte mAddrLo = (byte) (addr & 0x00FF);
                            byte mAddrHi = (byte) (addr / 256 & 0x00FF);
//                            log.log("New Read. Addr = " + mAddrLo);
                            master.readHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo));
                            appState = WAIT_DONE;
                            break;
                        }
                    } else {
                        appState = WAIT_AUTO_READ;
                        timerHandler.postDelayed(scanNodes, autoReadTime);
                    }
                }
                break;

            case WAIT_DONE:
                if(master.isDone()) {
                    mainActivity.notifyDataSetChanged();
                    if(master.getStatus()== ModbusMaster.ERROR){
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(master.getStatus()== ModbusMaster.TIMEOUT){
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Timeout", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else {// Case not error, jump to next node
                        node++;
                    }
                    appState = RUN;
                } else if(timeout) appState = START;
                break;

            case WAIT_AUTO_READ:
                if(isNewApp()||!isRunning()||!isAutoReadRR()) appState = STOP;
                else if (isScan()) {
                    appState = START;
                    setScan(false);
                    timerHandler.removeCallbacks(rTimeout);
                    timerHandler.postDelayed(rTimeout, timeoutTime);
                }
                break;

            case STOP:
                Snackbar.make(mainActivity.coordinatorLayoutView, "Long click on the Nodes you want to read.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                appState = START;
                timerHandler.removeCallbacks(rTimeout);
                timerHandler.removeCallbacks(scanNodes);
                setScan(false);
                result = true;
                break;

            default:
                appState = START;
        }

        return result;
    }

    private boolean appWR(){
        boolean result = false;
        switch (appState) {
            case START:
                node = mainActivity.getWrNode();
                wrReg = 0;
                int writeRegs = 0;
                for (int i = 0; i < MainActivity.numReg; i++) {
                    if (mainActivity.getIsWriteRegEnable(i)) writeRegs++;
                }
                if (writeRegs > 0) appState = RUN;
                else {
                    appState = STOP;
                    Snackbar.make(mainActivity.coordinatorLayoutView,
                            "Select at least one Register to Write!", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }

                break;

            case RUN:
                for (byte i = wrReg; i <= MainActivity.numReg; i++) {
                    if (i < MainActivity.numReg) {
                        if (mainActivity.getIsWriteRegEnable(i)) {
                            wrReg = i;
                            int addr = node * MainActivity.numReg + wrReg;
                            byte mAddrLo = (byte) (addr & 0x00FF);
                            byte mAddrHi = (byte) (addr / 256 & 0x00FF);
                            mLenLo = 1;
//                            log.log("New Write. reg = " + mAddrLo + " data = " + mainActivity.getWriteReg(i) + " i = " + i);
                            byte dataLo = (byte) (mainActivity.getWriteReg(i) & 0x00FF);
                            byte dataHi = (byte) (mainActivity.getWriteReg(i) / 256 & 0x00FF);
                            byte[] data = {dataHi, dataLo};
                            master.writeHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo), data);
                            appState = WAIT_RETURN;
                            break;
                        }
                    } else {
//                        log.log("Stop. i = " + i);
                        appState = STOP;
                    }

                }
                break;

            case WAIT_RETURN:
                if(master.isDone()) {
                    if(master.getStatus()== ModbusMaster.ERROR){
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(master.getStatus()== ModbusMaster.TIMEOUT){
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Timeout", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else {// Case not error, jump to next Register
                        wrReg++;
                    }
                    if(isNewApp()) appState = STOP;
                    else appState = RUN;
                } else if(timeout) appState = START;
                break;

            case STOP:
                appState = START;
                result = true;
                break;

            default:
                appState = START;
        }

        return result;
    }

    private boolean appSS(){
        boolean result = false;
        int addr = ssAddr;
        byte mAddrLo = (byte) (addr & 0x00FF);
        byte mAddrHi = (byte) (addr / 256 & 0x00FF);
        byte[] data;
//        log.log(appState);

        switch (appState) {
            case START:
                node = mainActivity.getSsNode();
                appState = START_SS;
                break;

            case START_SS:
                addr = ssAddr;
                mAddrLo = (byte) (addr & 0x00FF);
                mAddrHi = (byte) (addr / 256 & 0x00FF);
                mLenLo = 1; // Command + node address
                data = new byte[] {ssCmd,node};
                master.writeHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo), data);
                appState = WAIT_START;
                break;

            case WAIT_START:
                if(master.isDone()) {
                    if(master.getStatus()== ModbusMaster.ERROR){
                        appState = START;
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(master.getStatus()== ModbusMaster.TIMEOUT){
                        appState = START;
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Timeout", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else appState = READ_SS;
                    if(isNewApp()) appState = STOP_SS;
                } else if(timeout) appState = START;
                break;

            case READ_SS:
                addr = 0;
                mAddrLo = (byte) (addr & 0x00FF);
                mAddrHi = (byte) (addr / 256 & 0x00FF);
                mLenLo = 16;
//                log.log("New Read. Addr = " + mAddrLo);
                master.readHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo));
                appState = WAIT_READ_SS;
                break;

            case WAIT_READ_SS:
                if(master.isDone()) {
                    if(master.getStatus()== ModbusMaster.ERROR){
                        appState = READ_SS;
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(master.getStatus()== ModbusMaster.TIMEOUT){
                        appState = READ_SS;
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Timeout", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(!mainActivity.isRunning()) appState = STOP_SS;
                    else {
                        appState = READ_SS;
                        mainActivity.updateSSScreen();
                        timerHandler.removeCallbacks(rTimeout);
                        timerHandler.postDelayed(rTimeout, timeoutTime);
                    }
                    if(isNewApp()) appState = STOP_SS;
                } else if(timeout) appState = START;
                break;

            case STOP_SS:
                addr = ssAddr;
                mAddrLo = (byte) (addr & 0x00FF);
                mAddrHi = (byte) (addr / 256 & 0x00FF);
                mLenLo = 1; // Command + node address
                data = new byte[] {ssCmd,0};
                master.writeHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo), data);
                appState = WAIT_RETURN;
                break;

            case WAIT_RETURN:
                if(master.isDone()) {
                    if(master.getStatus()== ModbusMaster.ERROR){
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(master.getStatus()== ModbusMaster.TIMEOUT){
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Timeout", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else appState = STOP;
                } else if(timeout) appState = STOP_SS;
                break;

            case STOP:
                appState = START;
                result = true;
                break;

            default:
                appState = START;
        }

        return result;
    }

    private boolean appVT(){
        boolean result = false;
        switch (appState) {
            case START:
                node = mainActivity.getVtNode();
                appState = RUN;
                break;

            case RUN:
                mLenLo = 16;
                int addr = node * MainActivity.numReg;
                byte mAddrLo = (byte) (addr & 0x00FF);
                byte mAddrHi = (byte) (addr / 256 & 0x00FF);
//                log.log("New Read. Addr = " + mAddrLo);
                master.readHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo));
                appState = WAIT_DONE;
                break;

            case WAIT_DONE:
                if(master.isDone()) {
                    mainActivity.notifyDataSetChanged();
                    if (autoReadVT) {
                        timerHandler.postDelayed(scanNodes, autoReadTime);
                        appState = WAIT_AUTO_READ;
                    } else appState = STOP;
                } else if(timeout) appState = START;
                break;

            case WAIT_AUTO_READ:
                if(isNewApp()||!isRunning()||!isAutoReadVT()) appState = STOP;
                else if(isScan()) {
                    appState = START;
                    setScan(false);
                    timerHandler.removeCallbacks(rTimeout);
                    timerHandler.postDelayed(rTimeout, timeoutTime);
                }
                break;

            case STOP:
                appState = START;
                result = true;
                break;

            default:
                appState = START;
        }

        return result;
    }

    private boolean appIO(){
        boolean result = false;
        switch (appState) {
            case START:
                appState = RUN;
                break;

            case RUN:
                node = mainActivity.getIONode();
                mLenLo = 16;
                int addr = node * MainActivity.numReg;
                byte mAddrLo = (byte) (addr & 0x00FF);
                byte mAddrHi = (byte) (addr / 256 & 0x00FF);
                master.readHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo));
                appState = WAIT_DONE;
                break;

            case WAIT_DONE:
                if(master.isDone()) {
                    mainActivity.notifyDataSetChanged();
                    if(master.getStatus()== ModbusMaster.ERROR){
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(master.getStatus()== ModbusMaster.TIMEOUT){
                        master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Timeout", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    }else {
                        mainActivity.updateIOScreen();
                        if(!mainActivity.isRunning()) appState = STOP;
                        else {
                            appState = RUN;
                            timerHandler.removeCallbacks(rTimeout);
                            timerHandler.postDelayed(rTimeout, timeoutTime);
                        }
                    }
                } else if(isNewApp()) appState = STOP;
                else if(timeout) appState = START;
                break;

            case STOP:
                appState = START;
                timerHandler.removeCallbacks(rTimeout);
                setScan(false);
                result = true;
                break;

            default:
                appState = START;
        }

        return result;
    }


    // runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();

    Runnable mainRun = new Runnable() {

        @Override
        public void run() {
            main();
            //log.log("Main State = " + mainState + " App State = " + appState);
            timerHandler.postDelayed(mainRun, cycleTime);
        }
    };

    Runnable rTimeout = new Runnable() {

        @Override
        public void run() {
            timeout = true;
        }
    };

    Runnable scanNodes = new Runnable() {

        @Override
        public void run() {
            setScan(true);
        }
    };

    Runnable showStatus = new Runnable() {

        @Override
        public void run() {
            counter++;
            if(isRunning()&&false) {
                Snackbar.make(mainActivity.coordinatorLayoutView, "Main State = " + mainState + " App State = " + appState, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
            timerHandler.postDelayed(showStatus, reportTime);
        }
    };

    public void onStart(){
        if(!started) {
            timerHandler.postDelayed(mainRun, cycleTime);
            timerHandler.postDelayed(showStatus, reportTime);
            started = true;
        }
    }

    public void onStop(){
        started = false;
        if(mainActivity.usbComm !=null) {
            if (mainActivity.usbComm.getUartConfigured()) {
            master.stopComm();
            }
        }
        timerHandler.removeCallbacks(showStatus);
        timerHandler.removeCallbacks(scanNodes);
        timerHandler.removeCallbacks(mainRun);

    }

    public int getAutoReadTime() {
        return autoReadTime;
    }

    public void setAutoReadTime(int autoReadTime) {
        this.autoReadTime = autoReadTime;
    }

    public boolean isAutoReadRR() {
        return autoReadRR;
    }

    public void setAutoReadRR(boolean autoRead) {
        this.autoReadRR = autoRead;
    }

    public boolean isAutoReadVT() {
        return autoReadVT;
    }

    public void setAutoReadVT(boolean autoRead) {
        this.autoReadVT = autoRead;
    }

    public int getSsReadTime() {
        return ssReadTime;
    }

    public void setSsReadTime(int ssReadTime) {
        this.ssReadTime = ssReadTime;
    }

    public int getCurrentApp() {
        return currentApp;
    }

    public void setCurrentApp(int currentApp) {
        this.currentApp = currentApp;
        setNewApp(true);
    }

    public boolean isScan() {
        return scan;
    }

    public void setScan(boolean scan) {
        this.scan = scan;
    }

    public boolean isNewApp() {
        return newApp;
    }

    public void setNewApp(boolean newApp) {
        this.newApp = newApp;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setRunningTrue() {
        this.running = true;
    }

    public int getCycleTime() {
        return cycleTime;
    }

    public void setCycleTime(int cycleTime) {
        this.cycleTime = cycleTime;
    }

    public int getTimeoutTime() {
        return timeoutTime;
    }

    public void setTimeoutTime(int timeoutTime) {
        this.timeoutTime = timeoutTime;
    }

    public void startMaster(int USBBT){
        if(USBBT == MainActivity.USB) master = new ModbusMaster(mainActivity.usbComm, mainActivity.slave1);
        else if(USBBT == MainActivity.BLUETOOTH) master = new ModbusMaster(mainActivity.btComm, mainActivity.slave1);
    }
}
