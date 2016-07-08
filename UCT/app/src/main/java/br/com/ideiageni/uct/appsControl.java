package br.com.ideiageni.uct;

import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

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
    private static final int TIMEOUT = 99;

    private final int cycleTime = 200;
    private final int reportTime = 5000;

    private static final byte ssCmd = 32;

    private boolean ssEnable = false;
    private int currentApp = 999;

    private int mainState = 0;
    private int appState = 0;
    private MainActivity mContext;

    private byte node = 0;
    private final byte mLenHi = 0;
    private byte mLenLo = 16;
    private boolean autoRead = false;
    private int autoReadTime = 2000;
    private int ssReadTime = 500;
    private boolean scan = false;
    private boolean newApp = false;
    private boolean running = false;

    private OnScreenLog log = new OnScreenLog();

     public AppsControl (MainActivity context){
        this.mContext = context;
         onStart();
    }

    public void main(){
        int app;

        switch (mainState) {
            case HOLD:
                app = getCurrentApp();
                setNewApp(false);
                if(isRunning()) {
                    if (app == mContext.getAppConnect()) mainState = CONNECT;
                    if (app == mContext.getAppReadReg()) {
                        mainState = READ_REGISTERS;
                        log.log("New trasmission----------");
                    }
                    if (app == mContext.getAppWriteReg()) mainState = WRITE_REGISTERS;
                    if (app == mContext.getAppSiteSurvey()) mainState = SITE_SURVEY;
                }
                break;
            case CONNECT:
                mainState = HOLD;
                break;

            case READ_REGISTERS:
                if (appRR()) {
                    mainState = HOLD;
                    setRunning(false);
                }
                break;

            case WRITE_REGISTERS:
                mainState = HOLD;
                break;

            case SITE_SURVEY:
                mainState = HOLD;
                break;

            default:
                mainState = HOLD;
        }

    }

    private boolean appRR(){
        boolean result = false;
        switch (appState) {
            case START:
                node = 0;
                int readNodes = 0;
                for (int i = 0; i < mContext.getNumNodes(); i++) {
                    if (mContext.nodes.isReadEnable(i)) readNodes++;
                }
                if (readNodes > 0) appState = RUN;
                else {
                    appState = STOP;
                    Snackbar.make(mContext.coordinatorLayoutView, "Select at least one Node!"
                            + System.getProperty("line.separator")
                            + "Long click on the Nodes you want to read.", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }

                break;

            case RUN:
                for (byte i = node; i <= mContext.getNumNodes(); i++) {
                    if (i < mContext.getNumNodes()) {
                        if (mContext.nodes.isReadEnable(i)) {
                            node = i;
                            int addr = node * MainActivity.numReg;
                            byte mAddrLo = (byte) (addr & 0x00FF);
                            byte mAddrHi = (byte) (addr / 256 & 0x00FF);
                            log.log("New Read. Addr = " + mAddrLo);
                            mContext.master.readHR(new Modbus.commBundle(mAddrHi, mAddrLo, mLenHi, mLenLo));
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
                if(mContext.master.isDone()) {
                    if(mContext.master.getStatus()==ModbusControl.ERROR){
                        mContext.master.clearStatus();
                        Snackbar.make(mContext.coordinatorLayoutView, "Comm Error", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                } else if(mContext.master.getStatus()==ModbusControl.TIMEOUT){
                        mContext.master.clearStatus();
                    Snackbar.make(mContext.coordinatorLayoutView, "Comm Timeout", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                    } else {// Case not error, jump to next node
                        node++;
                    }
                    mContext.notifyDataSetChanged();
                    if(isNewApp()) appState = STOP;
                    else appState = RUN;

                } else timerHandler.postDelayed(mainRun, cycleTime);
                break;

            case WAIT_AUTO_READ:
                if(mContext.master.isDone()) {
                    mContext.notifyDataSetChanged();
                    if(isNewApp()) appState = STOP;
                    else if(isScan()) {
                        appState = START;
                        setScan(false);
                    }
                }
                break;

            case STOP:
                Snackbar.make(mContext.coordinatorLayoutView, "Long click on the Nodes you want to read.", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                appState = START;
                result = true;
                break;

            default:
                appState = START;
        }

        return result;
    }

//    public static void sendMessage(int app) {
//        if(getCurrentView() == vfSiteSurvey) {
//            if(isSSstarted()) {
//                node = 0;
//                mAddrLo = 0;
//                mAddrHi = 0;
//                mCmd = readMultipleCmd;
//
//                byte[] outData = slave1.readSend(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);
//                comm.SendMessage(outData);
//            } else {
//                mCmd = writeSingleCmd;
//                if(!isSSstarted()) node=0;
//                else node = ssNode;
//                int addr = ssAddr;
//                mAddrLo = (byte) (addr & 0x00FF);
//                mAddrHi = (byte) (addr / 256 & 0x00FF);
//                byte[] data = {ssCmd,node};
//                byte[] outData = slave1.writeSend(mCmd , mAddrHi, mAddrLo, data);
//                expectedBytes = outData.length-1;
//                comm.SendMessage(outData);
//
//            }
//        }
//    }

    // runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();

    Runnable mainRun = new Runnable() {

        @Override
        public void run() {
            main();
            timerHandler.postDelayed(mainRun, cycleTime);
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
            if(isRunning()&&false) {
                Snackbar.make(mContext.coordinatorLayoutView, "Main State = " + mainState + " App State = " + appState, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
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

    public void setRunning(boolean running) {
        this.running = running;
    }
}
