package br.com.ideiageni.startup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class Startup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("hu.sztupy.android.usbhostcontroller");
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            context.startActivity(launchIntent);//null pointer check in case package name was not found
            Toast.makeText(context, "Broadcast. App Loaded", Toast.LENGTH_LONG).show();
        } else Toast.makeText(context, "Broadcast. App not found", Toast.LENGTH_LONG).show();
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startMain);
    }

}