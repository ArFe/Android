package br.com.ideiageni.uct;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.widget.Toast;

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
    private byte mCmd;
    private byte mAddrHi;
    private byte mAddrLo;
    private byte mLenHi;
    private byte mLenLo;
    private byte[] sendData;

    private boolean busy = false;
    private boolean done = true;
    private int status = IDLE;

    private int timeOutTime = 2000;


    public ModbusControl(CommClass iComm, Modbus iSlave) {
        slave = iSlave;
        comm = iComm;
    }

    public boolean readHR (byte addrHi, byte addrLo, byte lenHi, byte lenLo){
        if(isDone()) {
            this.mCmd = Modbus.READMHR;
            this.mAddrHi = addrHi;
            this.mAddrLo = addrLo;
            this.mLenHi = lenHi;
            this.mLenLo = lenLo;

            setBusy(true);
            setDone(false);
            setStatus(READING);

            slave.readSend(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);

            sendData = slave.readSend(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo);
            comm.SendMessage(sendData);
            rt = new ReadThread();
            rt.execute(slave.getExpectedBytes());
            timerHandler.postDelayed(timeout, getTimeOutTime());

            return true;
        } else return false;

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
            int result = 0;
            if(status == READING) result = slave.readReceive(mCmd, mAddrHi, mAddrLo, mLenHi, mLenLo, readData);
            if(status == WRITING) result = slave.writeReceive(sendData, readData);

            if(result == Modbus.NOERROR) setStatus(READY);
            else setStatus(ERROR);

            setBusy(false);
            setDone(true);
            timerHandler.removeCallbacks(timeout);
        }
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
}
