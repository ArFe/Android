package br.com.ideiageni.uct;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by ariel on 07/07/2016.
 */
public class OnScreenLog {
    private static TextView tvLog;
    private static int logCount = 0;
    private static int logCountMax = 30;
    private static String[] logs = new String[logCountMax];

    public OnScreenLog(){}

    public OnScreenLog(Activity activity, int ViewID){
        tvLog = new TextView(activity.getApplicationContext());
        maintainLog("Log is working");
        tvLog.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        tvLog.setTextColor(Color.BLACK);
        tvLog.setBackgroundColor(Color.LTGRAY);
        tvLog.setAlpha((float) 0.4);

        LinearLayout linearLayout;
        RelativeLayout relativeLayout;
        try {
            linearLayout = (LinearLayout) activity.findViewById(ViewID);
        } catch (ClassCastException e) {linearLayout = null;};

        try {
            relativeLayout = (RelativeLayout) activity.findViewById(ViewID);
        } catch (ClassCastException e) {relativeLayout = null;};
        if(linearLayout != null) {
            linearLayout.addView(tvLog);
        } else if(relativeLayout != null) {
            relativeLayout.addView(tvLog);
        }
    }

    public void log (String text){
        String logText = text;
        maintainLog(logText);
    }

    public void log (int text){
        String logText = String.valueOf(text);
        maintainLog(logText);
    }

    public void log (int[] text){
        StringBuilder builder = new StringBuilder();
        for (int i : text) {
            builder.append(i);
            builder.append("-");
        }
        String logText = builder.toString();
        maintainLog(logText);
    }

    public void log (byte[] text){
        StringBuilder builder = new StringBuilder();
        for (int i : text) {
            builder.append(i);
            builder.append("-");
        }
        String logText = builder.toString();
        maintainLog(logText);
    }

    private void maintainLog(String newText){
        String logText = "";
        if(logCount<logCountMax) logCount++;
        for(int i=logCount-1; i>0; i--){
            logs[i] = logs[i-1];
        }
        logs[0] = newText;
        for(int i=0; i<logCount; i++){
            if(i<logCount-1) logText+=logs[i]+ System.getProperty("line.separator");
            else logText+=logs[i];
        }
        tvLog.setText(logText);
    }

    public void clearLog(){
        tvLog.setText("");
    }

    public void setLogVisible(boolean visibility){
        if(visibility) tvLog.setVisibility(View.VISIBLE);
        else tvLog.setVisibility(View.INVISIBLE);
    }

    public static int getLogCountMax() {
        return logCountMax;
    }

    public static void setLogCountMax(int logCountMax) {
        OnScreenLog.logCountMax = logCountMax;
        logs = new String[logCountMax];
    }
}
