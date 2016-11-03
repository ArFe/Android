package br.com.ideiageni.uct;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

/**
 * Created by ariel on 05/07/2016.
 */
public class ModbusMaster {

    static int IDLE = 0;
    static int READY = 1;
    static int READING = 3;
    static int WRITING = 6;
    static int TIMEOUT = 99;
    static int ERROR = 999;

    private USBCommClass usbComm;
    private BTCommClass btComm;
    private Modbus slave;
    private ReadThread rt;
    private byte mCmd = 90;
    private byte mAddrHi  = 91;
    private byte mAddrLo = 92;
    private byte mLenHi = 93;
    private byte mLenLo = 94;
    private byte[] sendData;

    private boolean busy = false;
    private boolean done = true;
    private int status = IDLE;
    private int USBBT = 0;

    private int timeOutTime = 2000;

    private Modbus.commBundle mBundle = new Modbus.commBundle(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);
    private Modbus.commBundle newBundle = new Modbus.commBundle(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);

    private OnScreenLog log = new OnScreenLog();

    public ModbusMaster(USBCommClass iComm, Modbus iSlave) {
        slave = iSlave;
        usbComm = iComm;
        USBBT = MainActivity.USB;
    }

    public ModbusMaster(BTCommClass iComm, Modbus iSlave) {
        slave = iSlave;
        btComm = iComm;
        USBBT = MainActivity.BLUETOOTH;
    }

    public boolean readHR (Modbus.commBundle bundle){
        if(isDone()) {
            mBundle = bundle;
            this.mCmd = Modbus.READMHR;
            this.mAddrHi = bundle.addrHi;
            this.mAddrLo = bundle.addrLo;
            this.mLenHi = bundle.lenHi;
            this.mLenLo = bundle.lenLo;

            setBusy(true);
            setDone(false);
            setStatus(READING);

            sendData = slave.readSend(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);
            //log.log(sendData);
            SendMessage(sendData);
            rt = new ReadThread();
            rt.execute(slave.getExpectedBytes());
            timerHandler.postDelayed(timeout, getTimeOutTime());

            return true;
        } else return false;

    }


    public boolean writeHR (Modbus.commBundle bundle, byte[] data){
        if(isDone()) {
            mBundle = bundle;
            this.mCmd = Modbus.WRITEMHR;
            this.mAddrHi = bundle.addrHi;
            this.mAddrLo = bundle.addrLo;
            this.mLenHi = bundle.lenHi;
            this.mLenLo = bundle.lenLo;

            setBusy(true);
            setDone(false);
            setStatus(WRITING);

            sendData = slave.writeSend(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo, data);
            SendMessage(sendData);
//            log.log(mCmd + " " + mAddrHi  + " " +  mAddrLo + " " +  mLenHi + " " +  mLenLo + " " +  data[1]  + " " + data[0]);
            rt = new ReadThread();
            rt.execute(slave.getExpectedBytes());
            timerHandler.postDelayed(timeout, getTimeOutTime());

            return true;
        } else return false;

    }
    // private class readThread  extends Thread
    public class ReadThread extends AsyncTask<Integer, Byte, byte[]> {

        @Override
        protected byte[] doInBackground(Integer... params) {
            int iAvailable = 0;
            int expectedBytes = params[0];

            Log.d("Mobbus Master", "bytes expected available = " + expectedBytes);
            while (iAvailable < expectedBytes) {
                iAvailable = getAvailable();
                if (isCancelled()) break;
            }
            Log.d("Mobbus Master", "bytes available = " + getAvailable());
            return ReceiveMessage(iAvailable);
        }

        @Override
        protected void onPostExecute(byte[] readData) {
            Log.d("Mobbus Master", "bytes available = " + getAvailable());
            log.log("bytes available = " + getAvailable());
            int result = 0;
            if(status == READING) result = slave.readReceive(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo, readData);
            if(status == WRITING) result = slave.writeReceive(sendData, readData);
//            log.log(readData);
//            log.log("On Post Execute. Addr = " + mAddrLo);

            if(result == Modbus.NOERROR) setStatus(READY);
            else setStatus(ERROR);

            setBusy(false);
            setDone(true);
            timerHandler.removeCallbacks(timeout);
        }
    }

    public void stopComm() {
        timerHandler.removeCallbacks(timeout);
        timerHandler.removeCallbacks(postStatus);
    }

    // runs without a timer by reposting this handler at the end of the runnable
    private Handler timerHandler = new Handler();
    private Runnable timeout = new Runnable() {

        @Override
        public void run() {
            rt.cancel(true);
            setStatus(TIMEOUT);
            setBusy(false);
            setDone(true);
            timerHandler.removeCallbacks(postStatus);
        }
    };
    private Runnable postStatus = new Runnable() {

        @Override
        public void run() {
            int iAvailable = getAvailable();
//            log.log("iAvailable " + iAvailable + " expectedBytes " + slave.getExpectedBytes());
            timerHandler.postDelayed(postStatus, 1000);
        }
    };

    private void SendMessage (byte[] sendData){
        if(USBBT == MainActivity.USB) usbComm.SendMessage(sendData);
        if(USBBT == MainActivity.BLUETOOTH) btComm.SendMessage(sendData);
    }

    private byte[] ReceiveMessage (int iAvailable){
        if(USBBT == MainActivity.USB) return usbComm.ReceiveMessage(iAvailable);
        if(USBBT == MainActivity.BLUETOOTH) return btComm.ReceiveMessage(iAvailable);
        else return null;
    }

    private int getAvailable (){
        if(USBBT == MainActivity.USB) return usbComm.getAvailable();
        if(USBBT == MainActivity.BLUETOOTH) return btComm.getAvailable();
        else return 0;
    }

    //Getters and Setters

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getTimeOutTime() {
        return timeOutTime;
    }

    public void setTimeOutTime(int timeOutTime) {
        this.timeOutTime = timeOutTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void clearStatus() {
        this.status = 0;
    }

    public Modbus.commBundle getCommBundle() {
        return mBundle;
    }

    public Modbus.commBundle getNewCommBundle() {
        return newBundle;
    }
}
