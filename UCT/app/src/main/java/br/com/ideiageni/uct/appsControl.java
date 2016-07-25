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
    private static final int VIBRATION_TEMPERATURE = 16;
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

    private byte node = 0;
    private byte wrReg = 0;
    private final byte mLenHi = 0;
    private byte mLenLo = 16;
    private boolean autoRead = false;
    private int autoReadTime = 2000;
    private int ssReadTime = 500;
    private boolean scan = false;
    private boolean newApp = false;
    private boolean running = false;
    private boolean timeout = false;

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
                app = getCurrentApp();
                setNewApp(false);
                if (app == mainActivity.getAppConnect()) mainState = CONNECT;
                else if (app == mainActivity.getAppReadReg()) mainState = READ_REGISTERS;
                else if (app == mainActivity.getAppWriteReg()) mainState = WRITE_REGISTERS;
                else if (app == mainActivity.getAppSiteSurvey()) mainState = SITE_SURVEY;
                else if (app == mainActivity.getAppVibTemp()) mainState = VIBRATION_TEMPERATURE;
//                log.log("App = " + app + " Vib temp = " + mainActivity.getAppVibTemp());
                timerHandler.postDelayed(rTimeout, timeoutTime);
                break;

            case CONNECT:
                mainState = STOP;
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

            case STOP:
                mainState = HOLD;
                setRunning(false);
                break;

            case TIMEOUT:
                mainState = HOLD;
                setRunning(false);
                timeout = false;
                onStop();
                log.log("Timeout");
                break;

            default:
                mainState = HOLD;
                setRunning(false);
        }

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
                            mainActivity.master.readHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo));
                            appState = WAIT_RETURN;
                            break;
                        }
                    } else {
                        if (autoRead) {
                            timerHandler.postDelayed(scanNodes, autoReadTime);
                            appState = WAIT_AUTO_READ;
                        } else appState = STOP;
                    }

                }
                break;

            case WAIT_RETURN:
                if(mainActivity.master.isDone()) {
                    if(mainActivity.master.getStatus()== ModbusMaster.ERROR){
                        mainActivity.master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(mainActivity.master.getStatus()== ModbusMaster.TIMEOUT){
                        mainActivity.master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Timeout", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else {// Case not error, jump to next node
                        node++;
                    }
                    mainActivity.notifyDataSetChanged();
                    if(isNewApp()) appState = STOP;
                    else appState = RUN;
                } else if(timeout) appState = START;
                break;

            case WAIT_AUTO_READ:
                if(mainActivity.master.isDone()) {
                    mainActivity.notifyDataSetChanged();
                    if(isNewApp()) appState = STOP;
                    else if(isScan()) {
                        appState = START;
                        setScan(false);
                        timerHandler.removeCallbacks(rTimeout);
                        timerHandler.postDelayed(rTimeout, timeoutTime);
                    }
                } else if(timeout) appState = START;
                break;

            case STOP:
                Snackbar.make(mainActivity.coordinatorLayoutView, "Long click on the Nodes you want to read.", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                appState = START;
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
                            mainActivity.master.writeHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo), data);
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
                if(mainActivity.master.isDone()) {
                    if(mainActivity.master.getStatus()== ModbusMaster.ERROR){
                        mainActivity.master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(mainActivity.master.getStatus()== ModbusMaster.TIMEOUT){
                        mainActivity.master.clearStatus();
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
                mainActivity.master.writeHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo), data);
                appState = WAIT_START;
                break;

            case WAIT_START:
                if(mainActivity.master.isDone()) {
                    if(mainActivity.master.getStatus()== ModbusMaster.ERROR){
                        appState = START;
                        mainActivity.master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(mainActivity.master.getStatus()== ModbusMaster.TIMEOUT){
                        appState = START;
                        mainActivity.master.clearStatus();
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
                mainActivity.master.readHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo));
                appState = WAIT_READ_SS;
                break;

            case WAIT_READ_SS:
                if(mainActivity.master.isDone()) {
                    if(mainActivity.master.getStatus()== ModbusMaster.ERROR){
                        appState = READ_SS;
                        mainActivity.master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(mainActivity.master.getStatus()== ModbusMaster.TIMEOUT){
                        appState = READ_SS;
                        mainActivity.master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Timeout", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(!mainActivity.isSsEnable()) appState = STOP_SS;
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
                mainActivity.master.writeHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo), data);
                appState = WAIT_RETURN;
                break;

            case WAIT_RETURN:
                if(mainActivity.master.isDone()) {
                    if(mainActivity.master.getStatus()== ModbusMaster.ERROR){
                        mainActivity.master.clearStatus();
                        Snackbar.make(mainActivity.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else if(mainActivity.master.getStatus()== ModbusMaster.TIMEOUT){
                        mainActivity.master.clearStatus();
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
                mainActivity.master.readHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo));
                if (autoRead) {
                    timerHandler.postDelayed(scanNodes, autoReadTime);
                    appState = WAIT_AUTO_READ;
                } else appState = STOP;
                break;

            case WAIT_AUTO_READ:
                if(mainActivity.master.isDone()) {
                    mainActivity.notifyDataSetChanged();
                    if(isNewApp()) appState = STOP;
                    else if(isScan()) {
                        appState = START;
                        setScan(false);
                        timerHandler.removeCallbacks(rTimeout);
                        timerHandler.postDelayed(rTimeout, timeoutTime);
                    }
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
        timerHandler.postDelayed(mainRun, cycleTime);
        timerHandler.postDelayed(showStatus, reportTime);
    }

    public void onStop(){
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

    public boolean isAutoRead() {
        return autoRead;
    }

    public void setAutoRead(boolean autoRead) {
        this.autoRead = autoRead;
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

    private void setRunning(boolean running) {
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
}
