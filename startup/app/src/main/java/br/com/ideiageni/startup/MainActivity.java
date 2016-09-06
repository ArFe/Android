package br.com.ideiageni.startup;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("hu.sztupy.android.usbhostcontroller");
//        if (launchIntent != null) {
//            launchIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
//            startActivity(launchIntent);//null pointer check in case package name was not found
//            Toast.makeText(this, "App Loaded", Toast.LENGTH_LONG).show();
//        } else Toast.makeText(this, "App not found", Toast.LENGTH_LONG).show();
//        finish();
//        Intent startMain = new Intent(Intent.ACTION_MAIN);
//        startMain.addCategory(Intent.CATEGORY_HOME);
//        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(startMain);
          Toast.makeText(this, "Main Activity", Toast.LENGTH_LONG).show();
    }
}
