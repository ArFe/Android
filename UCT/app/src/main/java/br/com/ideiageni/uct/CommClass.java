package br.com.ideiageni.uct;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

/**
 * Created by ariel on 30/11/2015.
 */
public class CommClass {

    public static D2xxManager ftD2xx = null;

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    //    D2xxManager ftdid2xx;
    private FT_Device ftDev = null;
    private int DevCount = -1;
    private int currentIndex = -1;
    private int openIndex = 0;

    /*local variables*/
    int baudRate = 19200; /*baud rate*/
    byte stopBit = 1; /*1:1stop bits, 2:2 stop bits*/
    byte dataBit = 8; /*8:8bit, 7: 7bit*/
    byte parity = 0;  /* 0: none, 1: odd, 2: even, 3: mark, 4: space*/
    byte flowControl = 0; /*0:none, 1: flow control(CTS,RTS)*/
    int portNumber = 1; /*port number*/

    private Context context;

    private boolean uartConfigured = false;


    public CommClass(Context comContext){
        context = comContext;
        try {
            ftD2xx = D2xxManager.getInstance(context);
        } catch (D2xxManager.D2xxException ex) {
            ex.printStackTrace();
        }
        SetupD2xxLibrary();
        ConfigDev();
        timerHandler.postDelayed(flushRun, 1000);

    }

    public boolean ConfigDev(){
        if(DevCount <= 0) createDeviceList();
        if((getftDevNull()||!ftDev.isOpen()) && DevCount > 0) connectFunction();
        if (!uartConfigured && !getftDevNull() && DevCount > 0) ConfigPort();
        return uartConfigured;
    }


    public int getAvailable() {
        return ftDev.getQueueStatus();
    }

    public byte[] Read(int numBytes) {
        byte[] readData = new byte[numBytes];
        ftDev.read(readData, numBytes);
        return readData;
    }

    public void flush(int numBytes) {
        byte[] readTemp = new byte[numBytes];
        ftDev.read(readTemp, numBytes);
        //ftDev.read(readTemp);
    }

    public boolean getUartConfigured() {
        return uartConfigured;
    }

    public boolean getftDevNull() {
        return ftDev == null;
    }

    public int getDevCount(){
        return DevCount;
    }
    public void createDeviceList() {
        int tempDevCount = ftD2xx.createDeviceInfoList(context);

        if (tempDevCount > 0){
            if( DevCount != tempDevCount ){
                DevCount = tempDevCount;
                updatePortNumberSelector();
            }
        }
        else{
            DevCount = -1;
            currentIndex = -1;
        }
    }

    public void SetConfig(int baud, byte dataBits, byte stopBits, byte iParity, byte iFlowControl){
        baudRate = baud;
        stopBit = stopBits;
        dataBit = dataBits;
        parity = iParity;
        flowControl = iFlowControl;

        ConfigPort();
    }


    public void ConfigPort(){
        if (!ftDev.isOpen()) {
            Log.e("j2xx", "SetConfig: device not open");
            return;
        }

        // configure our port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baudRate);

        switch (dataBit) {
            case 7:
                dataBit = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBit = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBit = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBit) {
            case 1:
                stopBit = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBit = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBit = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBit, stopBit, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        // TODO : flow ctrl: XOFF/XOM
        // TODO : flow ctrl: XOFF/XOM
        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);

        uartConfigured = true;
        Toast.makeText(context, "ConfigPort done " + ftDev.getQueueStatus(), Toast.LENGTH_SHORT).show();
    }

    public void updatePortNumberSelector()
    {
        //Toast.makeText(DeviceUARTContext, "updatePortNumberSelector:" + DevCount, Toast.LENGTH_SHORT).show();

        if(DevCount == 2)
        {
            Toast.makeText(context, "2 port device attached", Toast.LENGTH_SHORT).show();
            //portSpinner.setOnItemSelectedListener(new MyOnPortSelectedListener());
        }
        else if(DevCount == 4)
        {
            Toast.makeText(context, "4 port device attached", Toast.LENGTH_SHORT).show();
            //portSpinner.setOnItemSelectedListener(new MyOnPortSelectedListener());
        }
        else
        {
            Toast.makeText(context, "1 port device attached", Toast.LENGTH_SHORT).show();
            //portSpinner.setOnItemSelectedListener(new MyOnPortSelectedListener());
        }

    }


    private void SetupD2xxLibrary () {
    	/*
        PackageManager pm = getPackageManager();

        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
          Log.d("PackageList", "package: " + app.packageName + ", sourceDir: " + app.nativeLibraryDir);
          if (app.packageName.equals(R.string.app_name)) {
        	  System.load(app.nativeLibraryDir + "/libj2xx-utils.so");
        	  Log.i("ftd2xx-java","Get PATH of FTDI JIN Library");
        	  break;
          }
        }
        */
        // Specify a non-default VID and PID combination to match if required

        if(!ftD2xx.setVIDPID(0x0403, 0xada1))
            Log.i("ftd2xx-java", "setVIDPID Error");

    }

    public void disconnectFunction()
    {
//        DevCount = -1;
        currentIndex = -1;
//        uartConfigured = false;
//        openIndex = 0;
        try {
            Thread.sleep(50);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(ftDev != null){
            synchronized(ftDev){
                if(ftDev.isOpen()) ftDev.close();
            }
        }
    }

    public void connectFunction()
    {
        int tmpProtNumber = openIndex + 1;

        if( currentIndex != openIndex ){
            if(null == ftDev) {
                ftDev = ftD2xx.openByIndex(context, openIndex);
            }
            else {
                synchronized(ftDev){
                    ftDev = ftD2xx.openByIndex(context, openIndex);
                }
            }
            uartConfigured = false;
        }else{
            Toast.makeText(context, "Device port " + tmpProtNumber + " is already opened", Toast.LENGTH_SHORT).show();
            return;
        }

        if(ftDev == null)
        {
            Toast.makeText(context,"open device port("+tmpProtNumber+") NG, ftDev == null", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ftDev.isOpen())
        {
            currentIndex = openIndex;
            Toast.makeText(context, "open device port(" + tmpProtNumber + ") OK", Toast.LENGTH_SHORT).show();
            SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
        }
        else
        {
            Toast.makeText(context, "open device port(" + tmpProtNumber + ") NG", Toast.LENGTH_SHORT).show();
            //Toast.makeText(DeviceUARTContext, "Need to get permission!", Toast.LENGTH_SHORT).show();
        }
    }

    public byte[] ReceiveMessage(int iAvailable) {
        int i;
        byte[] readTemp;
        byte[] readData;

        synchronized (ftDev) {
            readData = Read(iAvailable);
            readTemp = new byte[iAvailable];
            for (i = 0; i < iAvailable; i++) {
                readTemp[i] = readData[i];
            }
        }
        return readTemp;
    }


    public void SendMessage(byte[] outData) {
        if (!ftDev.isOpen()) {
            Log.e("j2xx", "SendMessage: device not open");
            return;
        }

        ftDev.setLatencyTimer((byte) 16);

        ftDev.write(outData, outData.length);

    }

    Handler timerHandler = new Handler();
    Runnable flushRun = new Runnable() {

        @Override
        public void run() {
            int iAvailable = 0;
            if(uartConfigured) iAvailable = ftDev.getQueueStatus();
            if(iAvailable>0){
                flush(iAvailable);
                Toast.makeText(context, "Comm Class - Flushed", Toast.LENGTH_SHORT).show();
            }
            else timerHandler.postDelayed(this, 500);
        }
    };

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
