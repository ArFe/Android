package br.com.ideiageni.uct;

import android.os.AsyncTask;
import android.os.Handler;

/**
 * Created by ariel on 05/07/2016.
 */
public class ModbusControl {

    static int IDLE = 0;
    static int READY = 1;
    static int READING = 3;
    static int WRITING = 6;
    static int TIMEOUT = 99;
    static int ERROR = 999;

    private CommClass comm;
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

    private int timeOutTime = 2000;

    private Modbus.commBundle mBundle = new Modbus.commBundle(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);
    private Modbus.commBundle newBundle = new Modbus.commBundle(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);

    private OnScreenLog log = new OnScreenLog();

    public ModbusControl(CommClass iComm, Modbus iSlave) {
        slave = iSlave;
        comm = iComm;
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
            comm.SendMessage(sendData);
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

            while (iAvailable < expectedBytes) {
                iAvailable = comm.getAvailable();
//                if(iAvailable == 1) comm.flush(iAvailable);
                if (isCancelled()) break;
            }
            return comm.ReceiveMessage(iAvailable);
        }

        @Override
        protected void onPostExecute(byte[] readData) {
            int result = 0;
            if(status == READING) result = slave.readReceive(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo, readData);
            if(status == WRITING) result = slave.writeReceive(sendData, readData);
            log.log(readData);
            log.log("On Post Execute. Addr = " + mAddrLo);

            if(result == Modbus.NOERROR) setStatus(READY);
            else setStatus(ERROR);

            setBusy(false);
            setDone(true);
            timerHandler.removeCallbacks(timeout);
        }
    }

    public void stopComm() {
        timerHandler.removeCallbacks(timeout);

    }

    // runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();

    Runnable timeout = new Runnable() {

        @Override
        public void run() {
            rt.cancel(true);
            setStatus(TIMEOUT);
        }
    };


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
